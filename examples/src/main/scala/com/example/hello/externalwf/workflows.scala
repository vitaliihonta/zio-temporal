package com.example.hello.externalwf

import zio.*
import zio.temporal.*
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow.*

sealed trait OrderState extends Product with Serializable
object OrderState {
  case object Initial   extends OrderState
  case object Payed     extends OrderState
  case object Cancelled extends OrderState
}

class FoodOrderWorkflowImpl extends FoodOrderWorkflow {
  private val logger = ZWorkflow.getLogger(getClass)

  private val state = ZWorkflowState.make[OrderState](OrderState.Initial)

  override def order(goods: List[String], deliveryAddress: String): Boolean = {
    logger.info("Waiting until payment received or cancel or timeout...")
    val touched = ZWorkflow.awaitWhile(2.minutes)(state =:= OrderState.Initial)
    val deliveryWorkflow = ZWorkflow.newExternalWorkflowStub[FoodDeliveryWorkflow](
      FoodDeliveryWorkflow.makeId(ZWorkflow.info.workflowId)
    )
    if (!touched || state =:= OrderState.Cancelled) {
      ZExternalWorkflowStub.signal(
        deliveryWorkflow.cancelDelivery()
      )
      false
    } else {
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

sealed trait DeliveryState extends Product with Serializable
object DeliveryState {
  case object Initial                 extends DeliveryState
  case class Started(address: String) extends DeliveryState
  case object Cancelled               extends DeliveryState
}

// Define the parent workflow implementation. It implements the getGreeting workflow method
class FoodDeliveryWorkflowImpl extends FoodDeliveryWorkflow {
  private val logger = ZWorkflow.getLogger(getClass)

  private val state = ZWorkflowState.make[DeliveryState](DeliveryState.Initial)

  override def deliver(goods: List[String]): Boolean = {
    logger.info("Waiting until delivery start on cancel...")
    ZWorkflow.awaitWhile(state =:= DeliveryState.Initial)
    // If not cancelled at the moment, we assume delivery was successful
    state =!= DeliveryState.Cancelled
  }

  override def startDelivery(address: String): Unit = {
    logger.info(s"Delivery started. Address: $address")
    state := DeliveryState.Started(address)
  }

  override def cancelDelivery(): Unit = {
    logger.info("Delivery cancelled")
    state := DeliveryState.Cancelled
  }
}
