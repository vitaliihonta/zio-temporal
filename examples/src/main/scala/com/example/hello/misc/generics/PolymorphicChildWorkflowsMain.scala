package com.example.hello.misc.generics

import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.failure.{ApplicationFailure, ChildWorkflowFailure}
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}
import zio.temporal.workflow.*

trait NotificationsSenderWorkflow {
  @workflowMethod
  def send(msg: String): Unit
}

@workflowInterface
trait SmsNotificationsSenderWorkflow extends NotificationsSenderWorkflow
class SmsNotificationsSenderWorkflowImpl extends SmsNotificationsSenderWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def send(msg: String): Unit = {
    logger.info(s"SMS sent: $msg")
  }
}

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
  private val logger = ZWorkflow.makeLogger

  private val senders: List[ZChildWorkflowStub.Of[NotificationsSenderWorkflow]] =
    List(
      ZWorkflow
        .newChildWorkflowStub[PushNotificationsSenderWorkflow]
        .withRetryOptions(
          ZRetryOptions.default.withMaximumAttempts(2)
        )
        .build,
      ZWorkflow
        .newChildWorkflowStub[SmsNotificationsSenderWorkflow]
        .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(2))
        .build
    )

  override def send(msg: String): Unit = {
    senders.foldLeft(false) {
      case (sent @ true, _) => sent
      case (_, sender) =>
        try {
          // try to send
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
        ZWorker.addWorkflow[NotificationChildBasedWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[SmsNotificationsSenderWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[PushNotificationsSenderWorkflowImpl].fromClass

    def sendMsg(client: ZWorkflowClient, msg: String): TemporalIO[Unit] = for {
      workflowId <- ZIO.randomWith(_.nextUUID)
      notificationsWorkflow <- client
                                 .newWorkflowStub[NotificationChildBasedWorkflow]
                                 .withTaskQueue(TaskQueue)
                                 .withWorkflowId(workflowId.toString)
                                 .build
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
