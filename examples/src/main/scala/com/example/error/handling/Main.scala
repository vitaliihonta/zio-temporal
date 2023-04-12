package com.example.error.handling

import zio.*
import zio.temporal.*
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.logging.backend.SLF4J
import zio.temporal.json.JacksonDataConverter

object Main extends ZIOAppDefault {
  val TaskQueue = "math"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows = ZIO.serviceWithZIO[ZWorkerFactory] { workerFactory =>
      for {
        worker <- workerFactory.newWorker(TaskQueue)
        // NOTE: try typed/untyped activities and workflows
        activityImpl <- ZIO.service[TypedArithmeticActivityImpl]
        _ = worker.addActivityImplementation(activityImpl)
        _ = worker.addWorkflow[MathWorkflow].from(new TypedMathWorkflowImpl)
      } yield ()
    }

    val invokeWorkflows = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        workflowId <- Random.nextUUID
        mathWorkflow <- client
                          .newWorkflowStub[MathWorkflow]
                          .withTaskQueue(TaskQueue)
                          .withWorkflowId(workflowId.toString)
                          .withWorkflowExecutionTimeout(30.seconds)
                          .build
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
        ZLayer.succeed(ZWorkflowServiceStubsOptions.default),
        ZLayer.succeed(
          ZWorkflowClientOptions.default.withDataConverter(JacksonDataConverter.make())
        ),
        ZLayer.succeed(ZWorkerFactoryOptions.default),
        // NOTE: try typed/untyped activities
        ZLayer.fromFunction(new TypedArithmeticActivityImpl()(_: ZActivityOptions[Any])),
        ZWorkflowClient.make,
        ZActivityOptions.default,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }
}
