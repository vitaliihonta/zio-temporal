package com.example.misc

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.ZWorkflowExecutionMetadata
import zio.temporal.workflow._

object WorkflowExecutionsMain extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      _ <- ZWorkflowServiceStubs.setup()
      executions <- ZIO.serviceWithZIO[ZWorkflowClient] { client =>
                      client
                        .streamExecutions(
                          // try with/without query
                          query = Some(""" WorkflowType = "PaymentWorkflow" """)
                        )
                        .runCollect
                    }
      _ <- ZIO.logInfo(s"Found ${executions.size} executions")
      _ <- ZIO.foreach(executions)(printExecutionInfo)
    } yield ()

    program.provideSome[Scope](
      ZWorkflowServiceStubsOptions.make,
      ZWorkflowClientOptions.make,
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make
    )
  }

  private def printExecutionInfo(exec: ZWorkflowExecutionMetadata): ZIO[ZWorkflowClient, Throwable, Unit] =
    ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
      ZIO.logInfo(s"Found execution=$exec") *> workflowClient
        .streamHistory(exec.execution.workflowId)
        .tap(historyEvent => ZIO.logInfo(s"Event: $historyEvent"))
        .runDrain
    }
}
