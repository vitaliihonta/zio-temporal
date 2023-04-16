package com.example.heartbeatingactivity

import zio.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.logging.backend.SLF4J
import zio.temporal.activity.ZActivityOptions

object HeartbeatingActivityBatchWorker extends ZIOAppDefault {
  val TaskQueue = "HeartbeatingActivityBatch"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows = ZWorkerFactory.newWorker(TaskQueue) @@
      ZWorker.addActivityImplementationService[RecordProcessorActivity] @@
      ZWorker.addWorkflow[HeartbeatingActivityBatchWorkflowImpl].fromClass

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkflowServiceStubs.setup()
      // Poll workflow tasks
      _ <- ZWorkerFactory.serve
    } yield ()

    program
      .provideSome[Scope](
        ZLayer.succeed(ZWorkflowServiceStubsOptions.default),
        ZLayer.succeed(ZWorkflowClientOptions.default),
        ZLayer.succeed(ZWorkerFactoryOptions.default),
        // Activity
        RecordLoaderImpl.make,
        RecordProcessorImpl.make,
        RecordProcessorActivityImpl.make,
        // Services
        ZWorkflowClient.make,
        ZActivityOptions.default,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }
}
