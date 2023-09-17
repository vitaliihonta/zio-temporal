package com.example.generics

import zio._
import zio.logging.backend.SLF4J
import zio.temporal._
import zio.temporal.failure.{ApplicationFailure, ChildWorkflowFailure}
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}
import zio.temporal.workflow._

trait NotificationsSenderWorkflow {
  // NOTE: must have a @workflowMethod annotation WITHOUT an explicit name.
  // Otherwise, workflow subtypes will have the same workflowType
  @workflowMethod
  def send(msg: String): Unit
}

// NOTE: must have a @workflowInterface annotation
@workflowInterface
trait SmsNotificationsSenderWorkflow extends NotificationsSenderWorkflow
class SmsNotificationsSenderWorkflowImpl extends SmsNotificationsSenderWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def send(msg: String): Unit = {
    logger.info(s"SMS sent: $msg")
  }
}

// NOTE: must have a @workflowInterface annotation
@workflowInterface
trait PushNotificationsSenderWorkflow extends NotificationsSenderWorkflow

class PushNotificationsSenderWorkflowImpl extends PushNotificationsSenderWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def send(msg: String): Unit = {
    // just for demo purposes
    if (msg.contains("fail")) {
      throw ApplicationFailure.newFailure("Booom!", "PushFailure")
    }
    logger.info(s"PUSH sent: $msg")
  }
}

@workflowInterface
trait NotificationChildBasedWorkflow {
  @workflowMethod
  def send(msg: String): Unit
}

class NotificationChildBasedWorkflowImpl extends NotificationChildBasedWorkflow {
  private val logger         = ZWorkflow.makeLogger
  private val thisWorkflowId = ZWorkflow.info.workflowId

  // Using base Workflow type here
  private val senders: List[ZChildWorkflowStub.Of[NotificationsSenderWorkflow]] =
    List(
      ZWorkflow.newChildWorkflowStub[PushNotificationsSenderWorkflow](
        ZChildWorkflowOptions
          .withWorkflowId(s"$thisWorkflowId/push")
          .withRetryOptions(
            ZRetryOptions.default.withMaximumAttempts(2)
          )
      ),
      ZWorkflow.newChildWorkflowStub[SmsNotificationsSenderWorkflow](
        ZChildWorkflowOptions
          .withWorkflowId(s"$thisWorkflowId/sms")
          .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(2))
      )
    )

  override def send(msg: String): Unit = {
    senders.foldLeft(false) {
      case (sent @ true, _) => sent
      case (_, sender) =>
        try {
          // try to send
          logger.info(s"Trying ${sender.stubbedClass}...")
          ZChildWorkflowStub.execute(sender.send(msg))
          true
        } catch {
          // try the next sender if failed
          case _: ChildWorkflowFailure =>
            false
        }
    }
  }
}

object PolymorphicChildWorkflowsMain extends ZIOAppDefault {
  private val TaskQueue = "PolymorphicWorkflowExample"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflow =
      ZWorkerFactory.newWorker(TaskQueue) @@
        // register concrete workflow types
        ZWorker.addWorkflow[NotificationChildBasedWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[SmsNotificationsSenderWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[PushNotificationsSenderWorkflowImpl].fromClass

    def sendMsg(client: ZWorkflowClient, msg: String): TemporalIO[Unit] = for {
      workflowId <- ZIO.randomWith(_.nextUUID)
      notificationsWorkflow <- client.newWorkflowStub[NotificationChildBasedWorkflow](
                                 ZWorkflowOptions
                                   .withWorkflowId(workflowId.toString)
                                   .withTaskQueue(TaskQueue)
                               )
      _ <- ZWorkflowStub.execute(
             notificationsWorkflow.send(msg)
           )
    } yield ()

    val flow = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        // Push notification succeeds
        _ <- sendMsg(client, "push works")
        // Push notification fails, SMS notification succeeds
        _ <- sendMsg(client, "push fails sometimes, but SMS handles it")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflow
      _ <- ZWorkflowServiceStubs.setup()
      _ <- ZWorkerFactory.setup
      _ <- flow
    } yield ()

    program
      .provideSome[Scope](
        // temporal
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
        // options
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make
      )
  }
}
