package com.example.payments.workflows

import com.example.transactions._
import zio.temporal._

@workflowInterface
trait PaymentWorkflow {

  @workflowMethod
  def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView]

  @queryMethod
  def getStatus: Either[TransactionError, TransactionView]

  @signalMethod
  def confirmTransaction(command: ConfirmTransactionCommand): Unit
}
