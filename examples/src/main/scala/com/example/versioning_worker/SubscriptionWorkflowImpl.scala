package com.example.versioning_worker

import zio._
import zio.temporal.workflow._
import zio.temporal.activity._

class SubscriptionWorkflowImpl extends SubscriptionWorkflow {
  private val logger = ZWorkflow.makeLogger

  private val subscriptionActivities = ZWorkflow
    .newActivityStub[SubscriptionActivities]
    .withStartToCloseTimeout(5.minutes)
    .build

  override def proceedRecurringSubscription(subscriptionId: String): Unit = {
    logger.info(s"Processing subscription=$subscriptionId")
    val subscription = ZActivityStub.execute(
      subscriptionActivities.getSubscription(subscriptionId)
    )

    logger.info(s"Processing payment for subscription=$subscription")
    val paymentId = ZActivityStub.execute(
      subscriptionActivities.proceedPayment(subscriptionId, amount = subscription.amount)
    )
    logger.info(s"Payment with id=$paymentId processed successfully")
  }
}
