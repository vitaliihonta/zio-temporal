package com.example.heartbeatingactivity

import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.logging.backend.SLF4J
import zio.temporal.activity.ZActivityRunOptions

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
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make,
        ZActivityRunOptions.default,
        // Activity
        RecordLoaderImpl.make,
        RecordProcessorImpl.make,
        RecordProcessorActivityImpl.make,
        // Services
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }
}
