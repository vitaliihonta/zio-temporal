package com.example.heartbeatingactivity

import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*
import zio.logging.backend.SLF4J

object HeartbeatingActivityBatchStarter extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      _              <- ZWorkflowServiceStubs.setup()
      workflowClient <- ZIO.service[ZWorkflowClient]
      workflowId     <- ZIO.randomWith(_.nextUUID)

      batchWorkflow <- workflowClient
                         .newWorkflowStub[HeartbeatingActivityBatchWorkflow]
                         .withTaskQueue(HeartbeatingActivityBatchWorker.TaskQueue)
                         .withWorkflowId(workflowId.toString)
                         .build

      execution <- ZWorkflowStub.start(
                     batchWorkflow.processBatch()
                   )
      _ <- ZIO.logInfo(s"Started batch workflow workflowId=${execution.workflowId} runId=${execution.runId}")
    } yield ()

    program.provideSome[Scope](
      ZLayer.succeed(ZWorkflowServiceStubsOptions.default),
      ZLayer.succeed(ZWorkflowClientOptions.default),
      // Services
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make
    )
  }
}
