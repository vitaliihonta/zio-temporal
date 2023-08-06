package com.example.versioning

import zio.temporal._

@workflowInterface
trait SubscriptionWorkflow {

  @workflowMethod
  def proceedRecurringSubscription(subscriptionId: String): Unit
}

case class Subscription(
  id:        String,
  userEmail: String,
  amount:    BigDecimal)

@activityInterface
trait SubscriptionActivities {

  def getSubscription(subscriptionId: String): Subscription

  /** @return
    *   paymentId
    */
  def proceedPayment(subscriptionId: String, amount: BigDecimal): String

  def sendReceipt(subscriptionId: String, paymentId: String, email: String): Unit
}
