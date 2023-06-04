package com.example.heartbeatingactivity

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.logging.backend.SLF4J

object HeartbeatingActivityBatchCancelling extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      _              <- ZWorkflowServiceStubs.setup()
      workflowClient <- ZIO.service[ZWorkflowClient]
      workflowId     <- ZIO.consoleWith(_.readLine("Enter workflowId to cancel: "))

      batchWorkflow <- workflowClient.newWorkflowStub[HeartbeatingActivityBatchWorkflow](workflowId)
      _             <- batchWorkflow.cancel
      _             <- ZIO.logInfo(s"Cancelled workflowId=$workflowId")
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
