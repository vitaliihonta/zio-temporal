package com.example.monitoring

import com.uber.m3.tally.RootScopeBuilder
import io.micrometer.registry.otlp.{OtlpConfig, OtlpMeterRegistry}
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.{ContextPropagators, TextMapPropagator}
import io.temporal.common.reporter.MicrometerClientStatsReporter
import io.temporal.opentracing.{OpenTracingClientInterceptor, OpenTracingOptions, OpenTracingWorkerInterceptor}
import io.opentelemetry.opentracingshim.OpenTracingShim
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.extension.trace.propagation.OtTracePropagator
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes
import io.temporal.failure.TemporalException
import zio._
import zio.logging.backend.SLF4J
import zio.temporal._
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker._
import zio.temporal.workflow._

object MonitoringMain extends ZIOAppDefault {
  val TaskQueue = "hello-monitoring"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[SampleWorkflowImpl].fromClass @@
        ZWorker.addActivityImplementationService[SampleActivities]

    def invokeWorkflows(who: String): ZIO[ZWorkflowClient, TemporalException, Unit] =
      ZIO.serviceWithZIO[ZWorkflowClient] { client =>
        for {
          workflowId <- Random.nextUUID
          sampleWorkflow <- client
                              .newWorkflowStub[SampleWorkflow]
                              .withTaskQueue(TaskQueue)
                              .withWorkflowId(workflowId.toString)
                              .build

          result <- ZWorkflowStub.execute(
                      sampleWorkflow.greetAndBye(who)
                    )
          _ <- ZIO.logInfo(s"Invocation who=$who result=$result")
        } yield ()
      }

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkerFactory.setup
      _ <- ZWorkflowServiceStubs.setup()
      _ <- ZIO.foreachDiscard(List("Vitalii", "John", "Martin", "Victor", "Mike")) { who =>
             invokeWorkflows(who)
           }
      _ <- ZIO.logInfo("Give some time while metrics reported...")
      _ <- ZIO.sleep(10.seconds)
    } yield ()

    // monitoring setup
    val otlpConfig: OtlpConfig =
      Map(
        "url"                -> "http://localhost:4317",
        "resourceAttributes" -> "service.name=zio-temporal-sample-otel"
      ).get(_).orNull

    val metricsScope = new RootScopeBuilder()
      .reporter(
        new MicrometerClientStatsReporter(
          new OtlpMeterRegistry(
            otlpConfig,
            io.micrometer.core.instrument.Clock.SYSTEM
          )
        )
      )
      // it's usually better to use bigger intervals in production
      .reportEvery(5.seconds)

    val tracingOptions = buildOpenTracingOptions()

    val otlpClientInterceptor = new OpenTracingClientInterceptor(tracingOptions)

    val otlpWorkerInterceptor = new OpenTracingWorkerInterceptor(tracingOptions)

    program
      .provideSome[Scope](
        // activities
        SampleActivitiesImpl.make,
        ZActivityOptions.default,
        // temporal
        ZWorkflowServiceStubsOptions.make @@
          ZWorkflowServiceStubsOptions.withMetricsScope(metricsScope),
        ZWorkflowClientOptions.make @@
          ZWorkflowClientOptions.withInterceptors(otlpClientInterceptor),
        ZWorkerFactoryOptions.make @@
          ZWorkerFactoryOptions.withWorkerInterceptors(otlpWorkerInterceptor),
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }

  private def buildOpenTracingOptions(): OpenTracingOptions = {
    val selfResource = Resource.getDefault.merge(
      Resource.create(Attributes.of(ResourceAttributes.SERVICE_NAME, "zio-temporal-sample-otel"))
    )

    val spanProcessor = SimpleSpanProcessor.create(
      OtlpGrpcSpanExporter
        .builder()
        // Jaeger endpoint
        .setEndpoint("http://localhost:4317")
        .setTimeout(5.seconds)
        .build()
    )

    val tracerProvider = SdkTracerProvider
      .builder()
      .addSpanProcessor(spanProcessor)
      .setResource(selfResource)
      .build()

    val propagators = ContextPropagators.create(
      TextMapPropagator.composite(
        W3CTraceContextPropagator.getInstance(),
        OtTracePropagator.getInstance()
      )
    )

    OpenTracingOptions
      .newBuilder()
      .setTracer(
        OpenTracingShim.createTracerShim(
          OpenTelemetrySdk
            .builder()
            .setPropagators(propagators)
            .setTracerProvider(tracerProvider)
            .build()
        )
      )
      .build()
  }
}
