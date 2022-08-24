# Queries
A [Query](https://docs.temporal.io/workflows#queries) is a synchronous operation that is used to get the state of a Workflow Execution.  

## Defining query methods

Let's start from some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.state._
import zio.temporal.workflow._
import zio.temporal.activity._

import java.util.UUID
```

Consider following activity from the `Workflow state` section:

```scala mdoc:silent
@activityInterface
trait PaymentActivity {

  def debit(amount: BigDecimal, from: String): Unit

  def credit(amount: BigDecimal, to: String): Unit
}
```

We'll represent the payment state with such an enumeration:

```scala mdoc
sealed trait PaymentState
object PaymentState {
  case object Initial  extends PaymentState
  case object Debited  extends PaymentState
  case object Credited extends PaymentState
}
```

We can check our `PaymentWorkflow`'s current state using query methods:

```scala mdoc
@workflowInterface
trait PaymentWorkflow {

  @workflowMethod
  def proceed(amount: BigDecimal, from: String, to: String): Unit
  
  @queryMethod
  def getPaymentState(): PaymentState
}
```

Method for retrieving the Workflow Execution state should have an `@queryMethod` annotation.

Then we could implement a stateful workflow as follows:

```scala mdoc:silent
class PaymentWorkflowImpl extends PaymentWorkflow {
  private val paymentActivity = ZWorkflow
    .newActivityStub[PaymentActivity]
    .withStartToCloseTimeout(10.seconds)
    .build
    
  private val paymentState = ZWorkflowState.make[PaymentState](PaymentState.Initial)
  
  override def getPaymentState(): PaymentState = paymentState.snapshot
  
  override def proceed(amount: BigDecimal, from: String, to: String): Unit = {
    paymentActivity.debit(amount, from)
    paymentState := PaymentState.Debited
    
    // Let's add a pause here to emulate long-running workflow 
    ZWorkflow.sleep(5.seconds)
    
    paymentActivity.credit(amount, to)
    paymentState := PaymentState.Credited
  }
}
```

## Querying the state
Querying the workflow state is as simple as executing workflows!
First, you will need to create a Workflow stub and start the workflow:

```scala mdoc:silent
val transactionId = UUID.randomUUID().toString
val startWorkflow = ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  for {
    paymentWorkflow <- workflowClient
                        .newWorkflowStub[PaymentWorkflow]
                        .withTaskQueue("payment-queue")
                        .withWorkflowId(transactionId)
                        .withWorkflowRunTimeout(10.second)
                        .build
   _ <- ZWorkflowStub.execute(
          paymentWorkflow.proceed(amount = 42, from = "me",  to = "you")
        )
  } yield ()
}
```

While workflow is started inside one process, its state could be queried from another.  
That's why we need a way to retrieve the workflow state simply, by the Workflow ID.  

In Temporal, there is an `Untyped workflow stub` which shares this functionality.  
In `ZIO Temporal`, there is a type-safe wrapper called `Workflow stub proxy`.  

To create a stub proxy, you'll need the same Workflow client:

```scala mdoc:silent
val paymentWorkflowZIO = ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient.newWorkflowStubProxy[PaymentWorkflow](workflowId = transactionId)
}
```

Using the stub proxy, you can finally query the workflow state:

```scala mdoc:silent
val currentWorkflowStateZIO = for {
  _ <- startWorkflow
  paymentWorkflow <- paymentWorkflowZIO
  state <- ZWorkflowStub.query(paymentWorkflow.getPaymentState())
} yield state
```

Important notes:

- Due to Java SDK implementation, it's not allowed to just invoke the query method directly: this will throw an
  exception
- Reminder: querying workflow state = calling a remote server
- Those, you should wrap the query method invocation into a `ZWorkflowStub.query` block
