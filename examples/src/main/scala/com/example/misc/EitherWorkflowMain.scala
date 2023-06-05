package com.example.misc

import zio._
import zio.logging.backend.SLF4J
import zio.temporal._
import zio.temporal.activity.{ZActivity, ZActivityOptions, ZActivityStub}
import zio.temporal.worker._
import zio.temporal.workflow._

@workflowInterface
trait EitherWorkflow {

  @workflowMethod
  def start: Either[String, Int]
}

@activityInterface
trait EitherActivity {
  @activityMethod
  def either: Either[String, Int]

}

object EitherActivityImpl {
  val make: URLayer[ZActivityOptions[Any], EitherActivity] =
    ZLayer.fromFunction(EitherActivityImpl()(_: ZActivityOptions[Any]))
}

case class EitherActivityImpl()(implicit options: ZActivityOptions[Any]) extends EitherActivity {
  override def either: Either[String, Int] =
    ZActivity.run {
      ZIO.succeed(Right(41))
    }

}

case class EitherWorkflowImpl() extends EitherWorkflow {
  override def start: Either[String, Int] = {
    val stub = ZWorkflow
      .newActivityStub[EitherActivity]
      .withStartToCloseTimeout(1.seconds)
      .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1))
      .build

    // Deterministic random
    val randomSum = ZWorkflow.newRandom.nextInt()

    ZActivityStub.execute(stub.either).map(_ + randomSum)
  }
}

object EitherWorkflowMain extends ZIOAppDefault {
  val TaskQueue = "hello-either-workflows"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[EitherWorkflowImpl].fromClass @@
        ZWorker.addActivityImplementationService[EitherActivity]

    val invokeWorkflows = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        workflowId <- Random.nextUUID
        eitherWorkflow <- client
                            .newWorkflowStub[EitherWorkflow]
                            .withTaskQueue(TaskQueue)
                            .withWorkflowId(workflowId.toString)
                            .withRetryOptions(
                              ZRetryOptions.default.withMaximumAttempts(1)
                            )
                            .build
        _ <- ZIO.logInfo("Running workflow with either return type...")
        res <- ZWorkflowStub.execute(
                 eitherWorkflow.start
               )
        _ <- ZIO.logInfo(s"Workflow result: $res")
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
        ZWorkerFactory.make,
        EitherActivityImpl.make,
        ZActivityOptions.default
      )
  }
}
