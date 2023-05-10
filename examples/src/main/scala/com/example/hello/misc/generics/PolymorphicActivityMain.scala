package com.example.hello.misc.generics

import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.activity.{ZActivity, ZActivityOptions, ZActivityStub}
import zio.temporal.failure.{ActivityFailure, ApplicationFailure, ChildWorkflowFailure}
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}
import zio.temporal.workflow.*

trait NotificationsSenderActivity {
  def send(msg: String): Unit
}

@activityInterface
trait SmsNotificationsSenderActivity extends NotificationsSenderActivity {
  @activityMethod(name = "SendSMS")
  override def send(msg: String): Unit
}

object SmsNotificationsSenderActivityImpl {
  val make: URLayer[ZActivityOptions[Any], SmsNotificationsSenderActivity] =
    ZLayer.fromFunction(new SmsNotificationsSenderActivityImpl()(_: ZActivityOptions[Any]))
}

class SmsNotificationsSenderActivityImpl(implicit options: ZActivityOptions[Any])
    extends SmsNotificationsSenderActivity {
  override def send(msg: String): Unit = {
    ZActivity.run {
      ZIO.logInfo(s"SMS sent: $msg")
    }
  }
}

@activityInterface
trait PushNotificationsSenderActivity extends NotificationsSenderActivity {
  @activityMethod(name = "SendPush")
  override def send(msg: String): Unit
}

object PushNotificationsSenderActivityImpl {
  val make: URLayer[ZActivityOptions[Any], PushNotificationsSenderActivity] =
    ZLayer.fromFunction(new PushNotificationsSenderActivityImpl()(_: ZActivityOptions[Any]))
}

class PushNotificationsSenderActivityImpl(implicit options: ZActivityOptions[Any])
    extends PushNotificationsSenderActivity {
  override def send(msg: String): Unit = {
    ZActivity.run {
      ZIO.logInfo(s"Attempt to send push: $msg") *>
        ZIO.when(msg.contains("fail")) {
          ZIO.fail(ApplicationFailure.newFailure("Booom!", "PushFailure"))
        }
    }
  }
}

@workflowInterface
trait NotificationActivityBasedWorkflow {
  @workflowMethod
  def send(msg: String): Unit
}

class NotificationActivityBasedWorkflowImpl extends NotificationActivityBasedWorkflow {
  private val logger = ZWorkflow.makeLogger

  private val senders: List[ZActivityStub.Of[NotificationsSenderActivity]] =
    List(
      ZWorkflow
        .newActivityStub[PushNotificationsSenderActivity]
        .withStartToCloseTimeout(1.minute)
        .withRetryOptions(
          ZRetryOptions.default.withMaximumAttempts(2)
        )
        .build,
      ZWorkflow
        .newActivityStub[SmsNotificationsSenderActivity]
        .withStartToCloseTimeout(1.minute)
        .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(2))
        .build
    )

  override def send(msg: String): Unit = {
    senders.map(_.stubbedClass)
    senders.foldLeft(false) {
      case (sent @ true, _) => sent
      case (_, sender) =>
        try {
          // try to send
          ZActivityStub.execute(sender.send(msg))
          true
        } catch {
          // try the next sender if failed
          case _: ActivityFailure =>
            false
        }
    }
  }
}

object PolymorphicActivityMain extends ZIOAppDefault {
  private val TaskQueue = "PolymorphicActivityExample"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflow =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[NotificationActivityBasedWorkflowImpl].fromClass @@
        ZWorker.addActivityImplementationService[PushNotificationsSenderActivity] @@
        ZWorker.addActivityImplementationService[SmsNotificationsSenderActivity]

    def sendMsg(client: ZWorkflowClient, msg: String): TemporalIO[Unit] = for {
      workflowId <- ZIO.randomWith(_.nextUUID)
      notificationsWorkflow <- client
                                 .newWorkflowStub[NotificationActivityBasedWorkflow]
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
        // activity
        PushNotificationsSenderActivityImpl.make,
        SmsNotificationsSenderActivityImpl.make,
        // options
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make,
        ZActivityOptions.default
      )
  }
}
