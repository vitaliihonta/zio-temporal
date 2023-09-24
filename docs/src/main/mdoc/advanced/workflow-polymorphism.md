# Workflow polymorphism

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal workflow polymorphism" />
  <meta name="keywords" content="ZIO Temporal workflow polymorphism, Scala Temporal workflow polymorphism" />
</head>

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.failure.{ApplicationFailure, ChildWorkflowFailure}
```

## Workflow Interfaces with common parent
Imagine you need multiple workflows with the same interface. For instance, a notification sender.  
Possible implementations could be an SMS notification sender & Push notification sender.

Let's start by defining the workflow interface:

```scala mdoc:silent
// Intentionally not adding @workflowInterface annotation
trait NotificationsSenderWorkflow {
  // BUT the @workflowMethod annotation is here
  @workflowMethod
  def send(msg: String): Unit
}
```

Few notes here:
- By default, the Workflow Type name is the workflow **interface's simple name**.
  - The name is taken from the trait annotated with `@workflowInterface`
- **Do not specify** an alternative name in `@workflwoMethod`. It's not possible to override the name later 

Then, an additional step is required for this kind of polymorphism to work.  
We must define more "concrete" workflow interfaces:

```scala mdoc:silent
// NOTE: must have a @workflowInterface annotation
@workflowInterface
trait SmsNotificationsSenderWorkflow extends NotificationsSenderWorkflow

// NOTE: must have a @workflowInterface annotation
@workflowInterface
trait PushNotificationsSenderWorkflow extends NotificationsSenderWorkflow
```

The names for such Workflows would be `SmsNotificationsSenderWorkflow` and `PushNotificationsSenderWorkflow`.

Then we can implement those workflows. First, SMS notifications workflow:
```scala mdoc:silent
class SmsNotificationsSenderWorkflowImpl extends SmsNotificationsSenderWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def send(msg: String): Unit = {
    logger.info(s"SMS sent: $msg")
  }
}
```

Then, the Push notifications workflows
```scala mdoc:silent
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
```

With such a setup, let's demonstrate few usages

### Client side polymorphic invocation

```scala mdoc:silent
def basicExecute[W <: NotificationsSenderWorkflow](stub: ZWorkflowStub.Of[W]): TemporalIO[Unit] = {
    ZIO.logInfo("Executing notifications workflows") *>
      ZWorkflowStub.execute(stub.send("Hello, world!"))
    }

// Execute the client-side code
ZIO.serviceWithZIO[ZWorkflowClient] { client =>
  for {
    // Create stubs
    pushWorkflow <- client.newWorkflowStub[PushNotificationsSenderWorkflow](
                      ZWorkflowOptions
                        .withWorkflowId("SendPush")
                        .withTaskQueue("push")
                    )

    smsWorkflow <- client.newWorkflowStub[SmsNotificationsSenderWorkflow](
                     ZWorkflowOptions
                       .withWorkflowId("SendSMS")
                       .withTaskQueue("sms")
                   )

    // Execute the first workflow and recover to the second one
    _ <- basicExecute(pushWorkflow) orElse basicExecute(smsWorkflow)
  } yield ()
}
```

**Notes**
- Specify the upper-bound for the workflow interface
- Use `ZWorkflowStub.Of` wrapper type

### Child workflow polymorphic invocation:

```scala mdoc:silent
@workflowInterface
trait NotificationChildBasedWorkflow {
  @workflowMethod
  def send(msg: String): Unit
}

class NotificationChildBasedWorkflowImpl extends NotificationChildBasedWorkflow {
  private val logger = ZWorkflow.makeLogger
  private val workflowId = ZWorkflow.info.workflowId

  // Using base Workflow type here
  private val senders: List[ZChildWorkflowStub.Of[NotificationsSenderWorkflow]] =
    List(
      ZWorkflow.newChildWorkflowStub[PushNotificationsSenderWorkflow](
        ZChildWorkflowOptions
          .withWorkflowId(s"$workflowId/push")
          .withRetryOptions(
            ZRetryOptions.default.withMaximumAttempts(2)
          )
      ),
      ZWorkflow.newChildWorkflowStub[SmsNotificationsSenderWorkflow](
        ZChildWorkflowOptions
          .withWorkflowId(s"$workflowId/sms")
          .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(2))
      )
    )

  override def send(msg: String): Unit = {
    // Try each sender
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
```

**Notes**
- Use the base interface as the stub type
- Use `ZWorkflowStub.Of` wrapper type

