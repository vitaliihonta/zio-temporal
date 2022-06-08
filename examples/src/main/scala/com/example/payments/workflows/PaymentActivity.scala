package com.example.payments.workflows

import com.example.transactions._
import io.temporal.activity.ActivityInterface
import ztemporal.proto.ZUnit

@ActivityInterface
trait PaymentActivity {

  def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView]

  def verifyConfirmation(command: ConfirmTransactionCommand): Either[TransactionError, ZUnit]

  def cancelTransaction(command: CancelTransactionCommand): Either[TransactionError, ZUnit]
}
