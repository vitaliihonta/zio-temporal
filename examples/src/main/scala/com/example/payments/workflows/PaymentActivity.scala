package com.example.payments.workflows

import com.example.transactions._
import zio.temporal._

sealed abstract class BankError(message: String) extends Exception(message)
case class BankIsDownError()                     extends BankError("Bank is down")
case class InvalidConfirmationCodeError()        extends BankError("Invalid confirmation code")

@activityInterface
trait PaymentActivity {
  @throws[BankError]
  def proceed(transaction: ProceedTransactionCommand): TransactionView

  @throws[BankError]
  def verifyConfirmation(command: ConfirmTransactionCommand): Unit

  def cancelTransaction(command: CancelTransactionCommand): Unit
}
