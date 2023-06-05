package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.state._

@workflowInterface
trait PaymentWorkflow {
  // Returns true if confirmed
  @workflowMethod
  def processPayment(amount: BigDecimal): Boolean

  @signalMethod
  def confirm(): Unit
}

class PaymentWorkflowImpl extends PaymentWorkflow {

  private val isConfirmed = ZWorkflowState.make(false)

  override def processPayment(amount: BigDecimal): Boolean = {
    // Waits 10 minutes for the payment confirmation
    // Unblocks whenever the condition is met
    ZWorkflow.awaitUntil(10.minutes)(
      isConfirmed =:= true
    )
    isConfirmed.snapshot
  }

  override def confirm(): Unit = {
    // Confirmed!
    isConfirmed := true
  }
}
