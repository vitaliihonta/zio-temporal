# Observability

The observability section of the Temporal Developer's guide covers the many ways to view the current state of
your [Temporal Application](https://docs.temporal.io/temporal#temporal-application) that is, ways to view
which [Workflow Executions](https://docs.temporal.io/workflows#workflow-execution) are tracked by
the [Temporal Platform](https://docs.temporal.io/temporal#temporal-platform)
and the state of any specified Workflow Execution, either currently or at points of an execution.

This section covers features related to viewing the state of the application, including:

- Metrics
- Tracing
- Logging
- Visibility

## How to emit metrics

Each Temporal SDK is capable of emitting an optional set of metrics from either the Client or the Worker process. For a
complete list of metrics capable of being emitted, see the SDK metrics referenceLink preview icon.

Metrics can be scraped and stored in time series databases, such as:

- Prometheus
- M3db
- statsd

For more information about dasbharods, see
Temporal [Java SDK guide](https://docs.temporal.io/dev-guide/java/observability#metrics)

To emit metrics, use the `MicrometerClientStatsReporter` class to integrate with Micrometer `MeterRegistry`
configured for your metrics backend. Micrometer is a popular Java framework that provides integration with Prometheus
and other backends.

The following example shows how to use `MicrometerClientStatsReporter` to define the metrics scope and set it with the
`ZWorkflowServiceStubsOptions`.

**(1)** Add necessary dependencies

```scala
libraryDependencies ++= Seq(
  // Temporal integration with opentracing
  "io.temporal" % "temporal-opentracing" % "<temporal-version>",
  // Micrometer-otlp integration
  "io.micrometer" % "micrometer-registry-otlp" % "<micrometer-version>",
  // Opentelemetry libs
  "io.opentelemetry" % "opentelemetry-api"                         % "<otel-version>",
  "io.opentelemetry" % "opentelemetry-exporter-otlp"               % "<otel-version>",
  "io.opentelemetry" % "opentelemetry-extension-trace-propagators" % "<otel-version>",
  "io.opentelemetry" % "opentelemetry-opentracing-shim"            % "<otel-version>"
)
```

**(2)** Configure the Opentelemetry-based metrics registry & provide it to the `ZWorkflowServiceStubsOptions`:

```scala mdoc
import zio._
import zio.temporal._
import zio.temporal.workflow._
// required for metrics
import com.uber.m3.tally.RootScopeBuilder
import io.micrometer.registry.otlp.{OtlpConfig, OtlpMeterRegistry}
import io.temporal.common.reporter.MicrometerClientStatsReporter

// OtlpConfig is a SAM, so Map#get is easily convertable into OtlpConfig
val otlpConfig: OtlpConfig =
  Map(
    "url"                -> "http://otlp-server-endpoint:4317",
    "resourceAttributes" -> "service.name=<service-name>"
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

val workflowServiceStubsOptionsLayer =
  ZWorkflowServiceStubsOptions.make @@
    ZWorkflowServiceStubsOptions.withMetricsScope(metricsScope)
```

For more details,
find [Monitoring samples](https://github.com/vitaliihonta/zio-temporal/tree/main/examples/src/main/scala/com/example).
For details on configuring a OTLP scrape endpoint with Micrometer, see
Micrometer [OTLP doc](https://micrometer.io/docs/registry/otlp)

## How to setup Tracing

Tracing allows you to view the call graph of a Workflow along with its Activities and any Child Workflows.

Temporal Web's tracing capabilities mainly track Activity Execution within a Temporal context. If you need custom
tracing specific for your use case, you should make use of context propagation to add tracing logic accordingly.

Both client-side & worker-side tracing requires `OpenTracingOptions`. It can be build in the following way:
```scala mdoc
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.{ContextPropagators, TextMapPropagator}
import io.temporal.opentracing.OpenTracingOptions
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.opentracingshim.OpenTracingShim
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.resources.Resource
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.`export`.SimpleSpanProcessor
import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter
import io.opentelemetry.extension.trace.propagation.OtTracePropagator
import io.opentelemetry.semconv.ServiceAttributes

val tracingOptions: OpenTracingOptions = {
  val selfResource = Resource.getDefault.merge(
    Resource.create(Attributes.of(ServiceAttributes.SERVICE_NAME, "<resource-name>"))
  )
  
  val spanProcessor = SimpleSpanProcessor.create(
    OtlpGrpcSpanExporter
      .builder()
      .setEndpoint("http://otlp-server-endpoint:4317")
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
```

To configure tracing, register the `OpenTracingClientInterceptor` interceptor. You can register the interceptors
on both the Temporal Client side and the Worker side.

The following code examples demonstrate the `OpenTracingClientInterceptor` on the Temporal Client.

```scala mdoc
import io.temporal.opentracing.OpenTracingClientInterceptor

val otlpClientInterceptor = new OpenTracingClientInterceptor(tracingOptions)

val workflowClientOptionsLayer = ZWorkflowClientOptions.make @@
  ZWorkflowClientOptions.withInterceptors(otlpClientInterceptor)
```

The following code examples demonstrate the `OpenTracingWorkerInterceptor` on the Worker:
```scala mdoc
import io.temporal.opentracing.OpenTracingWorkerInterceptor
import zio.temporal.worker._

val otlpWorkerInterceptor = new OpenTracingWorkerInterceptor(tracingOptions)

val workerFactoryOptionsLayer = ZWorkerFactoryOptions.make @@
  ZWorkerFactoryOptions.withWorkerInterceptors(otlpWorkerInterceptor)
```

For more information, see the Temporal [OpenTracing module](https://github.com/temporalio/sdk-java/blob/master/temporal-opentracing/README.md)

## How to log from a Workflow

Send logs and errors to a logging service, so that when things go wrong, you can see what happened.

The SDK core uses WARN for its default logging level.

To get a standard `slf4j` logger in your Workflow code, use the `ZWorkflow.getLogger` method:

```scala mdoc:silent
import zio.temporal._
import zio.temporal.workflow._

@workflowInterface
trait MyWorkflow {
  // ...workflow methods
}

class MyWorkflowImpl extends MyWorkflow {
  private val logger = ZWorkflow.getLogger(getClass)
  // ...workflow methods
}
```

To avoid passing the current class, you can use `ZWorkflow.makeLogger` method:

```scala mdoc:silent
class MyWorkflowImpl2 extends MyWorkflow {
  private val logger = ZWorkflow.makeLogger
  // ...workflow methods
}
```

## How to use Visibility APIs

The term Visibility, within the Temporal Platform, refers to the subsystems and APIs that enable an operator to view
Workflow Executions that currently exist within a Cluster.

### How to use Search Attributes

The typical method of retrieving a Workflow Execution is by its Workflow Id.

However, sometimes you'll want to retrieve one or more Workflow Executions based on another property. For example,
imagine you want to get all Workflow Executions of a certain type that have failed within a time range, so that you can
start new ones with the same arguments.

You can do this with [Search Attributes](https://docs.temporal.io/visibility#search-attribute).

- Default Search Attributes like `WorkflowType`, `StartTime` and `ExecutionStatus` are automatically
  added to Workflow Executions.
- Custom Search Attributes can contain their own domain-specific data (like `customerId` or `numItems`).
    - A few generic Custom Search Attributes like `CustomKeywordField` and `CustomIntField` are created by
      default in Temporal's Docker Compose.

The steps to using custom Search Attributes are:

- Create a new Search Attribute in your Cluster using `tctl search-attribute create` or the Cloud UI.
- Set the value of the Search Attribute for a Workflow Execution:
    - On the Client by including it as an option when starting the Execution.
    - In the Workflow by calling `upsertSearchAttributes`.
- Read the value of the Search Attribute:
    - On the Client by calling `describeWorkflow`.
    - In the Workflow by looking at `workflowInfo`.
- Query Workflow Executions by the Search Attribute using
  a [List Filter](https://docs.temporal.io/visibility#list-filter):
- In `tctl`.
- In code by calling `listWorkflowExecutions`.

Here is how to query Workflow Executions:

### Search attribute types

ZIO Temporal encodes a lot of custom types on top of those supported by Temporal platform, including:

- Primitive types (`Int`, `Long`, `Double`, `Boolean`)
- `String`
- `UUID` (encoded as `keyword`)
- `BigInt`, `BigDecimal`
- `Option`
- Scala collections (`Set`, `List`, `Array`, etc.)
- Some `java.time` classes (`Instant`, `LocalDateTime`, `OffsetDateTime`)
- `Enumeratum` enums, `Scala 3` enums (as `keyword`)

Note that `String` can be encoded both as `text` and `keyword`. By default, it's `text`. If you need it to be encoded
as `keyword`, you must wrap it into `ZSearchAttribute.keyword` method.  
Other types encoded as `keyword` (such as `UUID`) should be wrapped as well.  
ZIO Temporal methods to set search attributes usually accept `Map[String, ZSearchAttribute]`.  
For simple types, just wrap them with `ZSearchAttribute()` method call, while `keyword`-based types should be wrapped
into `ZSearchAttribute.keyword` method:

```scala mdoc
import java.util.UUID
import java.time.LocalDateTime

val searchAttributes: Map[String, ZSearchAttribute] =
  Map(
    "TextAttr"     -> ZSearchAttribute("foo"),
    "KeywordAttr"  -> ZSearchAttribute.keyword("bar"),
    "KeywordAttr2" -> ZSearchAttribute.keyword(UUID.randomUUID()),
    "DateAttr"     -> ZSearchAttribute(LocalDateTime.now())
  )
```

`Enumeratum` enum example:

```scala mdoc:reset
import enumeratum.{Enum, EnumEntry}
import zio.temporal._
import zio.temporal.enumeratum._

sealed trait Color extends EnumEntry
object Color extends Enum[Color] {
  case object Red extends Color
  case object Green extends Color
  case object Blue extends Color

  override val values = findValues
}

val otherSearchAttributes: Map[String, ZSearchAttribute] = Map(
  "EnumAttr"    -> ZSearchAttribute.keyword[Color](Color.Green),
  "OptionEnum"  -> ZSearchAttribute.keyword(Option[Color](Color.Red)),
  "OptionEnum2" -> ZSearchAttribute.keyword(Option.empty[Color])
)
```

Same example with `Scala 3` enums
```scala
import zio.temporal._

enum Color {
  case Red, Green, Blue
}

val otherSearchAttributes: Map[String, ZSearchAttribute] = Map(
  "EnumAttr"    -> ZSearchAttribute.keyword[Color](Color.Green),
  "OptionEnum"  -> ZSearchAttribute.keyword(Option[Color](Color.Red)),
  "OptionEnum2" -> ZSearchAttribute.keyword(Option.empty[Color])
)
```

### How to set custom Search Attributes

After you've created custom Search Attributes in your Cluster (using` tctl search-attribute create` or the Cloud UI),
you can set the values of the custom Search Attributes when starting a Workflow.

To set a custom Search Attribute, call the `withSearchAttributes` method.

**(1)** Define the workflow

```scala mdoc:silent:reset
import zio.temporal._

@workflowInterface
trait MyWorkflow {
  @workflowMethod
  def someMethod(): Unit
}
```
**(2)** On the client side, create workflow options with search attributes:
```scala mdoc
import zio._
import zio.temporal._
import zio.temporal.workflow._

val workflowOptions = ZWorkflowOptions
  .withWorkflowId("<workflow-id>")
  .withTaskQueue("<task-queue>")
  .withSearchAttributes(
    Map(
      "CustomIntField"         -> ZSearchAttribute(1),
      "CustomBoolField"        -> ZSearchAttribute(true),
      "CustomKeywordField"     -> ZSearchAttribute.keyword("borsch"),
      "CustomKeywordListField" -> ZSearchAttribute.keyword(List("a", "bc", "def"))
    )
  )
```

**(3)** Create the workflow stub with those workflow options:
```scala mdoc:silent
val createWorkflow = ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient.newWorkflowStub[MyWorkflow](workflowOptions)
}
```

**(4)** You might set search attributes in the workflow implementation as well using `upsertSearchAttributes` method:

```scala mdoc:silent
import zio.temporal.workflow._

class MyWorkflowImpl extends MyWorkflow {
  override def someMethod(): Unit = {
    ZWorkflow.upsertSearchAttributes(
      Map(
        "OtherCustomAttribute" -> ZSearchAttribute(BigDecimal(10).pow(42)),
        "ExecutionDate"        -> ZSearchAttribute(ZWorkflow.currentTimeMillis)
      )
    )
  }
}
```

**(4)** In case you want to encode your custom type as a search attribute, it's required to define as implicit instance
of `ZSearchAttributeMeta` based on an existing type:

```scala mdoc
case class MyCustomType(value: String)

object MyCustomType {
  implicit val searchAttributeMeta: ZSearchAttributeMeta.Of[MyCustomType, ZSearchAttribute.Plain, String] =
    ZSearchAttributeMeta.string.convert(MyCustomType(_))(_.value)
}
```

### How to remove a Search Attribute from a Workflow

To remove a Search Attribute that was previously set, set it to an array `[]`.

To remove a Search Attribute, call the `upsertSearchAttributes` method and set it to an empty map.