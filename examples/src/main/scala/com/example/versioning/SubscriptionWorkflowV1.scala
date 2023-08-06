package com.example.versioning

import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.activity._

/** First version (the workflow logic evolved internally)
  */
class SubscriptionWorkflowV1 extends SubscriptionWorkflow {
  private val logger = ZWorkflow.makeLogger

  private val subscriptionActivities = ZWorkflow
    .newActivityStub[SubscriptionActivities]
    .withStartToCloseTimeout(5.minutes)
    .build

  private val self = ZWorkflow.newContinueAsNewStub[SubscriptionWorkflow].build

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

    ZWorkflow.sleep(5.minutes)

    // Change #1: send receipt. Uncomment it while the workflow is running
//    val version = ZWorkflow.version("add_send_receipt", minSupported = ZWorkflow.DefaultVersion, maxSupported = 1)
//
//    if (version == 1) {
//      logger.info(s"Sending receipt!")
//      ZActivityStub.execute(
//        subscriptionActivities.sendReceipt(
//          subscriptionId,
//          paymentId,
//          email = subscription.userEmail
//        )
//      )
//    }

    logger.info("Starting the next run")
    ZWorkflowContinueAsNewStub.execute(
      self.proceedRecurringSubscription(subscriptionId)
    )
  }
}
