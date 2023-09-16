package com.example.generics

import zio._
import zio.logging.backend.SLF4J
import zio.temporal._
import zio.temporal.activity.{ZActivity, ZActivityOptions, ZActivityRunOptions, ZActivityStub}
import zio.temporal.failure.{ActivityFailure, ApplicationFailure}
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}
import zio.temporal.workflow._

trait NotificationsSenderActivity {
  def send(msg: String): Unit
}

@activityInterface
trait SmsNotificationsSenderActivity extends NotificationsSenderActivity {
  // NOTE: must override the method signature & provide a UNIQUE activity name
  @activityMethod(name = "SendSMS")
  override def send(msg: String): Unit
}

object SmsNotificationsSenderActivityImpl {
  val make: URLayer[ZActivityRunOptions[Any], SmsNotificationsSenderActivity] =
    ZLayer.fromFunction(new SmsNotificationsSenderActivityImpl()(_: ZActivityRunOptions[Any]))
}

class SmsNotificationsSenderActivityImpl(implicit options: ZActivityRunOptions[Any])
    extends SmsNotificationsSenderActivity {
  override def send(msg: String): Unit = {
    ZActivity.run {
      ZIO.logInfo(s"SMS sent: $msg")
    }
  }
}

@activityInterface
trait PushNotificationsSenderActivity extends NotificationsSenderActivity {
  // NOTE: must override the method signature & provide a UNIQUE activity name
  @activityMethod(name = "SendPush")
  override def send(msg: String): Unit
}

object PushNotificationsSenderActivityImpl {
  val make: URLayer[ZActivityRunOptions[Any], PushNotificationsSenderActivity] =
    ZLayer.fromFunction(new PushNotificationsSenderActivityImpl()(_: ZActivityRunOptions[Any]))
}

class PushNotificationsSenderActivityImpl(implicit options: ZActivityRunOptions[Any])
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

  // Using base Workflow type here
  private val senders: List[ZActivityStub.Of[NotificationsSenderActivity]] =
    List(
      ZWorkflow
        .newActivityStub[PushNotificationsSenderActivity](
          ZActivityOptions
            .withStartToCloseTimeout(1.minute)
            .withRetryOptions(
              ZRetryOptions.default.withMaximumAttempts(2)
            )
        ),
      ZWorkflow
        .newActivityStub[SmsNotificationsSenderActivity](
          ZActivityOptions
            .withStartToCloseTimeout(1.minute)
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
        ZActivityRunOptions.default
      )
  }
}
