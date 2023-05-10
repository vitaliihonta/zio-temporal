package com.example.hello.misc.generics

import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.failure.{ApplicationFailure, ChildWorkflowFailure}
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}
import zio.temporal.workflow.*

trait NotificationsSender {
  @workflowMethod
  def send(msg: String): Unit
}

@workflowInterface
trait SmsNotificationsSender extends NotificationsSender
class SmsNotificationsSenderImpl extends SmsNotificationsSender {
  private val logger = ZWorkflow.makeLogger

  override def send(msg: String): Unit = {
    logger.info(s"SMS sent: $msg")
  }
}

@workflowInterface
trait PushNotificationsSender extends NotificationsSender

class PushNotificationsSenderImpl extends PushNotificationsSender {
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
trait NotificationWorkflow {
  @workflowMethod
  def send(msg: String): Unit
}

class NotificationWorkflowImpl extends NotificationWorkflow {
  private val logger = ZWorkflow.makeLogger

  private val senders: List[ZChildWorkflowStub.Of[NotificationsSender]] =
    List(
      ZWorkflow
        .newChildWorkflowStub[PushNotificationsSender]
        .withRetryOptions(
          ZRetryOptions.default.withMaximumAttempts(2)
        )
        .build,
      ZWorkflow
        .newChildWorkflowStub[SmsNotificationsSender]
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
  private val TaskQueue = "PolymorphicExample"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflow =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[NotificationWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[SmsNotificationsSenderImpl].fromClass @@
        ZWorker.addWorkflow[PushNotificationsSenderImpl].fromClass

    def sendMsg(client: ZWorkflowClient, msg: String): TemporalIO[Unit] = for {
      workflowId <- ZIO.randomWith(_.nextUUID)
      notificationsWorkflow <- client
                                 .newWorkflowStub[NotificationWorkflow]
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
