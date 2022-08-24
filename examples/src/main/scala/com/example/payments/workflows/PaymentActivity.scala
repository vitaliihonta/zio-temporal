package com.example.payments.workflows

import com.example.transactions._
import zio.temporal._

@activityInterface
trait PaymentActivity {

  def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView]

  def verifyConfirmation(command: ConfirmTransactionCommand): Either[TransactionError, Unit]

  def cancelTransaction(command: CancelTransactionCommand): Unit
}
