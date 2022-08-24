package com.example.payments.impl

import com.example.payments.workflows.PaymentActivity
import com.example.transactions._
import zio._
import zio.temporal.activity.ZActivity
import zio.temporal.activity.ZActivityOptions
import zio.temporal.protobuf.syntax._

object PaymentActivityImpl {
  val make: URLayer[ZActivityOptions[Any], PaymentActivity] =
    ZLayer.fromFunction(new PaymentActivityImpl()(_: ZActivityOptions[Any]))
}

class PaymentActivityImpl(implicit options: ZActivityOptions[Any]) extends PaymentActivity {

  override def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView] =
    ZActivity.run {
      proceedImpl(transaction)
    }

  override def verifyConfirmation(command: ConfirmTransactionCommand): Either[TransactionError, Unit] =
    ZActivity.run {
      verifyConfirmationImpl(command)
    }

  override def cancelTransaction(command: CancelTransactionCommand): Unit =
    ZActivity.run {
      cancelTransactionImpl(command)
    }

  private def proceedImpl(command: ProceedTransactionCommand): ZIO[Any, TransactionError, TransactionView] =
    for {
      _ <- ZIO.whenZIO(Random.nextIntBetween(1, 5).map(_ <= 2))(
             ZIO.fail(TransactionError(code = 42, message = "Failed to proceed transaction: you're unlucky"))
           )
      id <- Random.nextUUID
      transaction = TransactionView(
                      id = id.toProto,
                      status = TransactionStatus.InProgress,
                      description = "In progress",
                      sender = command.sender,
                      receiver = command.receiver,
                      amount = command.amount
                    )
      _ <- ZIO.logInfo(s"Created transaction=$transaction")
    } yield transaction

  private def verifyConfirmationImpl(command: ConfirmTransactionCommand): ZIO[Any, TransactionError, Unit] =
    if (command.confirmationCode != "42")
      ZIO.logError(s"Failed to proceed transaction_id=${command.id.fromProto}: invalid confirmation code") *>
        ZIO.fail(TransactionError(code = 6, message = "Please contact issuer bank"))
    else
      ZIO.logInfo(s"Successfully processed transaction_id=${command.id.fromProto}")

  private def cancelTransactionImpl(command: CancelTransactionCommand): UIO[Unit] =
    ZIO.logInfo(s"Cancelled transaction_id=${command.id.fromProto}")
}
