package com.example.misc

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.workflow.*

object WorkflowExecutionsMain extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      _ <- ZWorkflowServiceStubs.setup()
      executions <- ZIO.serviceWithZIO[ZWorkflowClient] { client =>
                      client.listExecutions(
                        // try with/without query
                        query = Some(""" WorkflowType = "PaymentWorkflow" """)
                      )
                    }
      _ <- ZIO.logInfo(s"Found ${executions.size} executions")
      _ <- ZIO.foreach(executions) { exec =>
             ZIO.logInfo(s"Exec: $exec")
           }
    } yield ()

    program.provideSome[Scope](
      ZWorkflowServiceStubsOptions.make,
      ZWorkflowClientOptions.make,
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make
    )
  }
}
