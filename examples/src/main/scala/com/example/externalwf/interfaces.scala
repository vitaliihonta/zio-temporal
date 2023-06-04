package com.example.externalwf

import zio.temporal._

@workflowInterface
trait FoodDeliveryWorkflow {
  @workflowMethod
  def deliver(goods: List[String]): Boolean

  @signalMethod
  def startDelivery(address: String): Unit

  @signalMethod
  def cancelDelivery(): Unit
}

object FoodDeliveryWorkflow {
  def makeId(orderId: String): String = orderId + "-delivery"
}

@workflowInterface
trait FoodOrderWorkflow {
  @workflowMethod
  def order(goods: List[String], deliveryAddress: String): Boolean

  @signalMethod
  def confirmPayed(): Unit

  @signalMethod
  def cancelOrder(): Unit
}
