package com.example.heartbeatingactivity

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.logging.backend.SLF4J

object HeartbeatingActivityBatchStarter extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      _              <- ZWorkflowServiceStubs.setup()
      workflowClient <- ZIO.service[ZWorkflowClient]
      workflowId     <- ZIO.randomWith(_.nextUUID)

      batchWorkflow <- workflowClient.newWorkflowStub[HeartbeatingActivityBatchWorkflow](
                         ZWorkflowOptions
                           .withWorkflowId(workflowId.toString)
                           .withTaskQueue(HeartbeatingActivityBatchWorker.TaskQueue)
                       )

      execution <- ZWorkflowStub.start(
                     batchWorkflow.processBatch()
                   )
      _ <- ZIO.logInfo(s"Started batch workflow workflowId=${execution.workflowId} runId=${execution.runId}")
    } yield ()

    program.provideSome[Scope](
      ZWorkflowServiceStubsOptions.make,
      ZWorkflowClientOptions.make,
      // Services
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make
    )
  }
}
