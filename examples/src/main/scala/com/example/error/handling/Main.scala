package com.example.error.handling

import zio._
import zio.temporal._
import zio.temporal.activity.ZActivityRunOptions
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {
  val TaskQueue = "math"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addActivityImplementationService[ArithmeticActivity] @@
        ZWorker.addWorkflow[MathWorkflow].from(new TypedMathWorkflowImpl)

    val invokeWorkflows = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        workflowId <- Random.nextUUID
        mathWorkflow <- client.newWorkflowStub[MathWorkflow](
                          ZWorkflowOptions
                            .withWorkflowId(workflowId.toString)
                            .withTaskQueue(TaskQueue)
                            .withWorkflowExecutionTimeout(30.seconds)
                        )
        _ <- ZIO.logInfo("Running math workflow!")
        res <- ZWorkflowStub.execute(
                 mathWorkflow.formula(4)
               )
        _ <- ZIO.logInfo(s"Math workflow result: $res")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkerFactory.setup
      _ <- ZWorkflowServiceStubs.setup()
      _ <- invokeWorkflows
    } yield ()

    program
      .provideSome[Scope](
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make,
        // NOTE: try typed/untyped activities
        TypedArithmeticActivityImpl.make,
        ZWorkflowClient.make,
        ZActivityRunOptions.default,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }
}
