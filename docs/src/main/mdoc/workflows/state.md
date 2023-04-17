# Workflow state

Temporal allows your workflows to be stateful.  
Basically, it means that you could store the workflow state inside a plain Scala variable in the workflow
implementation.

## Using workflow state

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

Consider following workflow and activity:

```scala mdoc:silent
@activityInterface
trait PaymentActivity {

  def debit(amount: BigDecimal, from: String): Unit

  def credit(amount: BigDecimal, to: String): Unit
}

@workflowInterface
trait PaymentWorkflow {

  @workflowMethod
  def proceed(amount: BigDecimal, from: String, to: String): Unit
}
```

While implementing `PaymentWorkflow`, we may be interested in the current payment state: is it on `debit` or `credit`
step?
Let's model it using the following enum:

```scala mdoc
sealed trait PaymentState
object PaymentState {
  case object Initial  extends PaymentState
  case object Debited  extends PaymentState
  case object Credited extends PaymentState
}
```

Then we could implement a stateful workflow as follows:

```scala mdoc:silent
class PaymentWorkflowImpl extends PaymentWorkflow {
  private val paymentActivity: ZActivityStub.Of[PaymentActivity] = ZWorkflow
    .newActivityStub[PaymentActivity]
    .withStartToCloseTimeout(10.seconds)
    .build
    
  private val paymentState = ZWorkflowState.make[PaymentState](PaymentState.Initial)
  
  override def proceed(amount: BigDecimal, from: String, to: String): Unit = {
    ZActivityStub.execute(
      paymentActivity.debit(amount, from)
    )
    
    paymentState := PaymentState.Debited
    
    ZActivityStub.execute(
      paymentActivity.credit(amount, to)
    )
    
    paymentState := PaymentState.Credited
  }
}
```

While it's allowed to use plain Scala `var`, it's recommended to use `ZWorkflowState` instead.  
First, it allows to deal with uninitialized state (aka null):

#### Uninitialized state
```scala mdoc:silent
val missingState = ZWorkflowState.empty[PaymentState]
```

#### Check whenever the state is initialized
```scala mdoc
missingState.isEmpty
```

Second, it provides a lot of handy methods for dealing with mutable state:

#### Set the state
```scala mdoc:silent
missingState := PaymentState.Initial
```
```scala mdoc
missingState.snapshot
```

#### Update the state on certain conditions
```scala mdoc:silent
def condition: Boolean = true
missingState.setToIf(condition)(PaymentState.Debited)
```
```scala mdoc
missingState.snapshot
```

#### Update using partial functions
```scala mdoc:silent
missingState.updateWhen {
  case PaymentState.Debited => PaymentState.Credited
}
```
```scala mdoc
missingState.snapshot
```

#### Equality checks:
```scala mdoc
missingState =:= PaymentState.Credited

missingState =!= PaymentState.Debited
```