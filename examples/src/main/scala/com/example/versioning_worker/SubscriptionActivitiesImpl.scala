package com.example.versioning_worker

import zio._
import zio.temporal.activity._

object SubscriptionActivitiesImpl {
  val make: URLayer[ZActivityOptions[Any], SubscriptionActivities] =
    ZLayer.fromFunction(new SubscriptionActivitiesImpl()(_: ZActivityOptions[Any]))
}

class SubscriptionActivitiesImpl()(implicit options: ZActivityOptions[Any]) extends SubscriptionActivities {
  override def getSubscription(subscriptionId: String): Subscription = {
    ZActivity.run {
      for {
        _ <- ZIO.logInfo(s"Getting subscription=$subscriptionId")
        email = "person@example.com"
        amount <- Random.nextIntBetween(100, 500)
      } yield Subscription(
        id = subscriptionId,
        userEmail = email,
        amount = amount
      )
    }
  }

  override def proceedPayment(subscriptionId: String, amount: BigDecimal): String = {
    ZActivity.run {
      for {
        _ <- ZIO.logInfo(s"Proceeding subscription=$subscriptionId amount=$amount")
        _ <- ZIO.sleep(5.seconds)
      } yield {
        s"payment-$subscriptionId"
      }
    }
  }
}
