package com.example.versioning

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

  override def sendReceipt(subscriptionId: String, paymentId: String, email: String): Unit = {
    ZActivity.run {
      for {
        _ <- ZIO.logInfo(s"Sending receipt subscription=$subscriptionId payment=$paymentId to=$email")
        _ <- ZIO.sleep(5.seconds)
      } yield ()
    }
  }
}
