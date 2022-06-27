package com.example.payments.workflows

import com.example.transactions._
import zio.temporal._
import zio.temporal.proto.ZUnit

@activity
trait PaymentActivity {

  def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView]

  def verifyConfirmation(command: ConfirmTransactionCommand): Either[TransactionError, ZUnit]

  def cancelTransaction(command: CancelTransactionCommand): Either[TransactionError, ZUnit]
}
