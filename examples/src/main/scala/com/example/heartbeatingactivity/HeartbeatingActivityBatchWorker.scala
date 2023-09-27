package com.example.heartbeatingactivity

import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.logging.backend.SLF4J
import zio.temporal.activity.{ZActivityImplementationObject, ZActivityRunOptions}

object HeartbeatingActivityBatchWorker extends ZIOAppDefault {
  val TaskQueue = "HeartbeatingActivityBatch"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val activitiesLayer: URLayer[
      RecordLoader with RecordProcessor with ZActivityRunOptions[Any],
      List[ZActivityImplementationObject[_]]
    ] = ZLayer.collectAll(
      List(
        ZActivityImplementationObject.layer(RecordProcessorActivityImpl.make),
        ZActivityImplementationObject.layer(ReporterActivityImpl.make)
      )
    )

    val registerWorkflows = ZWorkerFactory.newWorker(TaskQueue) @@
      ZWorker.addActivityImplementationsLayer(activitiesLayer) @@
      ZWorker.addWorkflow[HeartbeatingActivityBatchWorkflowImpl].fromClass

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkflowServiceStubs.setup()
      // Poll workflow tasks
      _ <- ZWorkerFactory.serve
    } yield ()

    program
      .provideSome[Scope](
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make,
        ZActivityRunOptions.default,
        // Activity
        RecordLoaderImpl.make,
        RecordProcessorImpl.make,
        // Services
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }
}
