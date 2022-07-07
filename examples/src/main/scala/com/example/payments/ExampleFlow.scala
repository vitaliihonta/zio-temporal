package com.example.payments

import zio._
import com.example.payments.service.PaymentService

object ExampleFlow {
  val make: URLayer[PaymentService, ExampleFlow] = ZLayer.fromFunction(new ExampleFlow(_))
}

class ExampleFlow(paymentService: PaymentService) {

  def proceedPayment(): UIO[Unit] = {
    val paymentFlow = for {
      sender        <- Random.nextUUID
      receiver      <- Random.nextUUID
      amount        <- Random.nextDoubleBetween(100.0, 1000.0)
      transactionId <- paymentService.createPayment(sender, receiver, amount)
      _             <- simulateUserActivity
      // Try to change the confirmation code to see what happens
      _ <- paymentService.confirmPayment(transactionId, confirmationCode = "42")
      _ <- simulateUserActivity
      result <- (ZIO.sleep(100.millis) *> paymentService.getStatus(transactionId))
                  .repeatUntil(_.isFinished)
      _ <- ZIO.logInfo(s"End-up polling status, fetching the result, result=$result")
    } yield ()

    paymentFlow.catchAll { error =>
      ZIO.logError(s"Error processing transaction: $error")
    }
  }

  private def simulateUserActivity: UIO[Unit] =
    ZIO.logInfo("User is thinking...") *>
      ZIO.sleep(1.second)
}
