# Signals
A [Signal](https://docs.temporal.io/workflows#signals) is a message sent to a running Workflow Execution.  
Signals deliver data to a running Workflow Execution. They're used to interact with Workflows, e.g. to update their state.  
Refer to [Temporal documentation](https://docs.temporal.io/workflows#signals) for more details regarding signals and how they're handled by Temporal..  

## Defining signal methods

Let's start from some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.temporal.activity._

import java.util.UUID
```

Consider following activity from the previous section:

```scala mdoc:silent
@activityInterface
trait PaymentActivity {

  def debit(amount: BigDecimal, from: String): Unit

  def credit(amount: BigDecimal, to: String): Unit
}
```

We'll improve our existing payment workflow, so that it have a confirmation step:

```scala mdoc
sealed trait PaymentState
object PaymentState {
  case object Initial                 extends PaymentState
  case object Debited                 extends PaymentState
  case class  Confirmed(code: String) extends PaymentState
  case object Credited                extends PaymentState
}
```

For implementing payment confirmation, we could use signal methods:

```scala mdoc
@workflowInterface
trait PaymentWorkflow {

  @workflowMethod
  def proceed(amount: BigDecimal, from: String, to: String): Unit
  
  @signalMethod
  def confirmPayment(code: String): Unit
  
  @queryMethod
  def getPaymentState(): PaymentState
}
```

Method for handling signals should have an `@signalMethod` annotation.

Then we could implement a stateful workflow as follows:

```scala mdoc:silent
class PaymentWorkflowImpl extends PaymentWorkflow {
  private val paymentActivity = ZWorkflow
    .newActivityStub[PaymentActivity]
    .withStartToCloseTimeout(10.seconds)
    .build
    
  private var paymentState: PaymentState = PaymentState.Initial 
  
  override def getPaymentState(): PaymentState = paymentState
  
  override def confirmPayment(code: String): Unit = {
    paymentState = PaymentState.Confirmed(code)
  }
  
  override def proceed(amount: BigDecimal, from: String, to: String): Unit = {
    paymentActivity.debit(amount, from)
    paymentState = PaymentState.Debited
    
    // Waiting for the confirmation
    ZWorkflow.awaitWhile(paymentState == PaymentState.Debited)
    
    paymentActivity.credit(amount, to)
    paymentState = PaymentState.Credited
  }
}
```

## Signaling workflows
Sending signals to workflows is pretty straightforward!
First, you will need to start the workflow:

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

Using a stub proxy, you can finally query the workflow state:

```scala mdoc:silent
val runWorkflow = for {
  _ <- startWorkflow
  
  paymentWorkflow <- ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
    workflowClient.newWorkflowStubProxy[PaymentWorkflow](workflowId = transactionId)
  }
  
  stateBefore <- ZWorkflowStub.query(paymentWorkflow.getPaymentState())
  
  _ <- ZWorkflowStub.signal(
         paymentWorkflow.confirmPayment(code = "1234")
       )
  
  stateAfter <- ZWorkflowStub.query(paymentWorkflow.getPaymentState())
} yield ()
```

Important notes:

- Due to Java SDK implementation, it's not allowed to just invoke the signal method directly: this will throw an
  exception
- Reminder: signalling workflow state = calling a remote server
- Those, you should wrap the signal method invocation into a `ZWorkflowStub.signal` block
