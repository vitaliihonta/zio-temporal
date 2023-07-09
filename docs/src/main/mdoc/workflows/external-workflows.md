# External workflows

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal external workflows" />
  <meta name="keywords" content="ZIO Temporal external workflows, Scala Temporal external workflows" />
</head>

A Workflow execution may interact with other workflow executions even if it's not a child.  
The _second_ workflow is **External** to the _first_ one.  
The _first_ workflow may send signals or cancel the **External** workflow.  

## Defining child workflows

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
// First workflow
@workflowInterface
trait FoodDeliveryWorkflow {
  @workflowMethod
  def deliver(goods: List[String]): Unit

  @signalMethod
  def startDelivery(address: String): Unit
  
  @signalMethod
  def cancelDelivery(): Unit
}

object FoodDeliveryWorkflow {
  // A helper function to get a stable workflowId
  def makeId(orderId: String): String = orderId + "-delivery"
}

// The second workflow
@workflowInterface
trait FoodOrderWorkflow {
  // Returns true when completed
  @workflowMethod
  def order(goods: List[String], deliveryAddress: String): Boolean

  @signalMethod
  def confirmPayed(): Unit

  @signalMethod
  def cancelOrder(): Unit
}

```

Let's imagine those workflows are started in parallel by some other workflow.  
To avoid communicating through the parent workflow, they can simply interact with each other.  
From the perspective of `FoodOrderWorkflow`, the `FoodDeliveryWorkflow` is **External**.  

Let's first define `FoodDeliveryWorkflow`
```scala mdoc:silent
// Implementation details don't matter in this example
class FoodDeliveryWorkflowImpl extends FoodDeliveryWorkflow {
  private val logger = ZWorkflow.makeLogger
  
  override def deliver(goods: List[String]): Unit = {
    logger.info("Delivering...")
    // Do something ...
  }

  override def startDelivery(address: String): Unit = {
    logger.info(s"Delivery started. Address: $address")
  }

  override def cancelDelivery(): Unit = {
    logger.info("Delivery cancelled")
  }
}
```

Then the `FoodOrderWorkflow`:
```scala mdoc:silent
// The order has a state
sealed trait OrderState extends Product with Serializable
object OrderState {
  case object Initial   extends OrderState
  case object Payed     extends OrderState
  case object Cancelled extends OrderState
}

class FoodOrderWorkflowImpl extends FoodOrderWorkflow {
  private val logger = ZWorkflow.makeLogger

  // The workflow state
  private val state = ZWorkflowState.make[OrderState](OrderState.Initial)

  override def order(goods: List[String], deliveryAddress: String): Boolean = {
    logger.info("Waiting until payment received or cancel or timeout...")
    // Wait for condition to be met
    val touched = ZWorkflow.awaitWhile(2.minutes)(state =:= OrderState.Initial)
    
    // Create an external workflow stub
    val deliveryWorkflow: ZExternalWorkflowStub.Of[FoodDeliveryWorkflow] = 
      ZWorkflow.newExternalWorkflowStub[FoodDeliveryWorkflow](
        // Must know the exact workflow ID
        FoodDeliveryWorkflow.makeId(ZWorkflow.info.workflowId)
      )
    
    if (!touched || state =:= OrderState.Cancelled) {
      // Sending signal
      ZExternalWorkflowStub.signal(
        deliveryWorkflow.cancelDelivery()
      )
      false
    } else {
      // Sending signal
      ZExternalWorkflowStub.signal(
        deliveryWorkflow.startDelivery(deliveryAddress)
      )
      true
    }
  }

  override def confirmPayed(): Unit = {
    logger.info("The order was payed")
    state := OrderState.Payed
  }

  override def cancelOrder(): Unit = {
    logger.info("The order was cancelled")
    state := OrderState.Cancelled
  }

}
```

- To create an external workflow stub, you must use `ZWorkflow.newExternalWorkflowStub[<WorkflowType>]` method.
  - The workflow ID must be known to create the stub
- **Reminder: you must always** wrap the external workflow signal invocation into `ZExternalWorkflowStub.signal` method.
    - `deliveryWorkflow.cancelDelivery()` invocation would be re-written into an untyped Temporal's signal invocation
    - A direct method invocation will throw an exception