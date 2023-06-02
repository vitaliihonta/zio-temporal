package com.example.cancellation

import zio.*
import zio.temporal.*
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {
  val TaskQueue = "hello-cancellation"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addActivityImplementationService[GreetingActivities] @@
        ZWorker.addWorkflow[GreetingWorkflowImpl].fromClass

    val invokeWorkflows = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        workflowId <- Random.nextUUID
        greetingWorkflow <- client
                              .newWorkflowStub[GreetingWorkflow]
                              .withTaskQueue(TaskQueue)
                              .withWorkflowId(workflowId.toString)
                              .build
        _ <- ZIO.logInfo("Running greeting with cancellation workflow!")
        res <- ZWorkflowStub.execute(
                 greetingWorkflow.getGreeting("World")
               )
        _ <- ZIO.logInfo(s"Greeting received: $res")
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
        GreetingActivitiesImpl.make,
        ZWorkflowClient.make,
        ZActivityOptions.default,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }
}
