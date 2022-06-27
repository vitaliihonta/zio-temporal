package com.example.payments.impl

import com.example.payments.workflows.PaymentActivity
import com.example.transactions._
import logstage.LogIO
import zio._
import zio.temporal.activity.ZActivity
import zio.temporal.activity.ZActivityOptions
import zio.temporal.proto.ZUnit
import zio.temporal.proto.syntax._

class PaymentActivityImpl(logger: LogIO[UIO])(implicit options: ZActivityOptions) extends PaymentActivity {

  override def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView] =
    ZActivity.run {
      proceedImpl(transaction)
    }

  override def verifyConfirmation(command: ConfirmTransactionCommand): Either[TransactionError, ZUnit] =
    ZActivity.run {
      verifyConfirmationImpl(command)
    }

  override def cancelTransaction(command: CancelTransactionCommand): Either[TransactionError, ZUnit] =
    ZActivity.run {
      cancelTransactionImpl(command)
    }

  private def proceedImpl(command: ProceedTransactionCommand): ZIO[ZEnv, TransactionError, TransactionView] =
    for {
      _ <- ZIO.whenM(random.nextIntBetween(1, 5).map(_ <= 2))(
             ZIO.fail(TransactionError(code = 42, message = "Failed to proceed transaction: you're unlucky"))
           )
      id <- random.nextUUID
      transaction = TransactionView(
                      id = id.toProto,
                      status = TransactionStatus.InProgress,
                      description = "In progress",
                      sender = command.sender,
                      receiver = command.receiver,
                      amount = command.amount
                    )
      _ <- logger.info(s"Created $transaction")
    } yield transaction

  private def verifyConfirmationImpl(command: ConfirmTransactionCommand): ZIO[ZEnv, TransactionError, Unit] =
    if (command.confirmationCode != "42")
      logger.error(s"Failed to proceed ${command.id.fromProto -> "transaction_id"}: invalid confirmation code") *>
        ZIO.fail(TransactionError(code = 6, message = "Please contact issuer bank"))
    else
      logger.info(s"Successfully processed ${command.id.fromProto -> "transaction_id"}")

  private def cancelTransactionImpl(command: CancelTransactionCommand): ZIO[ZEnv, TransactionError, Unit] =
    logger.info(s"Cancelled ${command.id.fromProto -> "transaction_id"}")
}
