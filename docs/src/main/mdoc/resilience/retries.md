# Timeouts and retries

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal retries" />
  <meta name="keywords" content="ZIO Temporal retries, Scala Temporal retries" />
</head>

A Retry Policy works in cooperation with the timeouts to provide fine controls to optimize the execution experience.  
In Temporal, you could define retry policies for both Activity execution and Workflow execution.  
Refer to [Temporal documentation](https://docs.temporal.io/retry-policies) for more details regarding retries.

## Activity retries

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc
import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.temporal.activity._

import java.util.UUID
```

Consider following simple activity and workflow definitions:

```scala mdoc
@activityInterface
trait BookingActivity {
  def bookFlight(name: String, surname: String, flightNumber: String): UUID /*Booking ID*/ 
  
  def purchaseFlight(bookingId: UUID, cardId: UUID): UUID /*Booking ID*/ 
}

@workflowInterface
trait BookingWorkflow {
  @workflowMethod
  def bookFlight(name: String, surname: String, flightNumber: String, cardId: UUID): UUID /*Booking ID*/
}
```

When declaring activities inside the workflow implementation, it's allowed to provide custom timeouts and retry policies:
```scala mdoc
class BookingWorkflowImpl extends BookingWorkflow {
  private val bookingActivity: ZActivityStub.Of[BookingActivity] = ZWorkflow
    .newActivityStub[BookingActivity]
    .withStartToCloseTimeout(10.seconds)
    .withRetryOptions(
      ZRetryOptions.default
        .withMaximumAttempts(3)
        .withInitialInterval(300.millis)
        .withMaximumAttempts(5)
        .withBackoffCoefficient(0.5)
    )
    .build
    
  override def bookFlight(name: String, surname: String, flightNumber: String, cardId: UUID): UUID = 
    ???
}
```

Important notes:
- `withStartToCloseTimeout` allows to specify the maximum duration of a single [Activity Task Execution](https://docs.temporal.io/tasks/#activity-task-execution)
- `withRetryOptions` allows to specify the retry policy for an activity execution
- `withMaximumAttempts` limits the number of retries
- `withInitialInterval`, `withMaximumAttempts` and `withBackoffCoefficient` adds a backoff

## Workflow retries
Adding retry policies for workflows is pretty the same as for activities:

```scala mdoc:silent
ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient
    .newWorkflowStub[BookingWorkflow]
    .withTaskQueue("booking")
    .withWorkflowId("<ANY ID>")
    .withWorkflowExecutionTimeout(5.minutes)
    .withWorkflowRunTimeout(10.seconds)
    .withRetryOptions(
      ZRetryOptions.default.withMaximumAttempts(5)
    )
    .build
}
```

Important notes:
- `withWorkflowExecutionTimeout` allows to specify the maximum time that a Workflow Execution could be in progress, including retries and `Continue as New` (more details [here](https://docs.temporal.io/workflows/#workflow-execution-timeout))
- `withWorkflowRunTimeout` allows to specify the maximum run time of a single [Workflow run](https://docs.temporal.io/concepts/what-is-a-workflow-execution/#workflow-execution-chain)
- `withRetryOptions` allows to specify the retry policy in the same way as for activities