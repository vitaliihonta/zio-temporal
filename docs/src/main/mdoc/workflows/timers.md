# Timers
A Workflow can set a durable timer for a fixed time period. A Workflow can sleep for months.  
Timers are persisted, so even if your Worker or Temporal Cluster is down when the time period completes, as soon as your Worker and Cluster are back up, the `sleep()` call will resolve and your code will continue executing.

Sleeping is a resource-light operation: it does not tie up the process, and you can run millions of Timers off a single Worker.

## Using timers

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.state._
import java.util.UUID
```

Then define workflow interfaces:

```scala mdoc:silent
@workflowInterface
trait DumbPaymentWorkflow {
  // Returns false if cancelled
  @workflowMethod
  def processPayment(amount: BigDecimal): Boolean
  
  @signalMethod
  def cancel(): Unit
}
```
An example of a simple timer is `ZWorkflow.sleep(<duration>)`:

```scala mdoc:silent
class DumbPaymentWorkflowImpl extends DumbPaymentWorkflow {
  
  private val isCancelled = ZWorkflowState.make(false)
  
  override def processPayment(amount: BigDecimal): Boolean = {
    // Give the customer time to change her mind:
    ZWorkflow.sleep(10.minutes)
    
    // Do some stuff
    // ...
    isCancelled.snapshot
  }
  
  override def cancel(): Unit = {
    // Cancelled!
    isCancelled := true
  }
}
```

More complicated timer can wait for a condition to be met. It's called `ZWorkflow.awaitUntil`:
```scala mdoc:silent
@workflowInterface
trait BetterPaymentWorkflow {
  // Returns true if confirmed
  @workflowMethod
  def processPayment(amount: BigDecimal): Boolean
  
  @signalMethod
  def confirm(): Unit
}

class BetterPaymentWorkflowImpl extends BetterPaymentWorkflow {

  private val isConfirmed = ZWorkflowState.make(false)

  override def processPayment(amount: BigDecimal): Boolean = {
    // Wait forever until the payment is confirmed
    ZWorkflow.awaitUntil(isConfirmed =:= true)

    // Do some stuff
    // ...
    isConfirmed.snapshot
  }

  override def confirm(): Unit = {
    // Confirmed!
    isConfirmed := true
  }
}
```

**Notes**:
**(1)** `ZWorkflow.awaitUntil` also accepts a timeout:
```scala mdoc:silent
class EvenBetterPaymentWorkflowImpl extends BetterPaymentWorkflow {

  private val isConfirmed = ZWorkflowState.make(false)

  override def processPayment(amount: BigDecimal): Boolean = {
    // Waits 10 minutes for the payment confirmation
    // Unblocks whenever the condition is met
    val wasConfirmed = ZWorkflow.awaitUntil(10.minutes)(
      isConfirmed =:= true
    )
    // wasConfirmed = false in case if timed out
    if (wasConfirmed) {
      // Do some stuff
      // ...
    }
    isConfirmed.snapshot
  }

  override def confirm(): Unit = {
    // Confirmed!
    isConfirmed := true
  }
}
```
**(2)** There is also other variant of this timer called `ZWorkflow.awaitWhile`
