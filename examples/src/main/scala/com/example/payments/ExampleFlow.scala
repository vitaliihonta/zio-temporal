package com.example.payments

import zio.*
import com.example.payments.service.{PaymentError, PaymentService, Transaction}

import java.util.UUID

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
      // We're simulating a user doing something with the payment...
      _ <- userActivity(transactionId).forkDaemon
      // Status poller
      result <- pollStatus(transactionId)
      _      <- ZIO.logInfo(s"Transaction result=$result")
    } yield ()

    paymentFlow.catchAll { error =>
      ZIO.logError(s"Error processing transaction: $error")
    }
  }

  private def pollStatus(transactionId: UUID): IO[PaymentError, Transaction] =
    (
      ZIO.sleep(2.seconds) *>
        ZIO.logInfo("Checking transaction status...") *>
        paymentService.getState(transactionId)
    ).repeatWhile(_.isEmpty)
      .map(_.get)

  private def userActivity(transactionId: UUID): IO[PaymentError, Unit] =
    for {
      _ <- simulateUserActivity
      // Options to play with to see what happens:
      // 1. Try to change the confirmation code
      // 2. Try to comment the confirmPayment invocation
      _ <- paymentService.confirmPayment(transactionId, confirmationCode = "42")
    } yield ()

  private def simulateUserActivity: UIO[Unit] =
    ZIO.logInfo("User is thinking...") *>
      ZIO.sleep(5.seconds)
}
