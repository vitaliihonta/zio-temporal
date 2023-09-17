package com.example.child

import zio._
import zio.temporal._
import zio.temporal.activity.ZActivityRunOptions
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {
  val TaskQueue = "hello-child-workflows"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[GreetingWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[GreetingChildImpl].fromClass

    val invokeWorkflows = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        workflowId <- Random.nextUUID
        greetingWorkflow <- client.newWorkflowStub[GreetingWorkflow](
                              ZWorkflowOptions
                                .withWorkflowId(workflowId.toString)
                                .withTaskQueue(TaskQueue)
                            )
        _ <- ZIO.logInfo("Running greeting with child workflow!")
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
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }
}
