package com.example.payments.workflows

import com.example.transactions._
import zio.temporal._

@workflowInterface
trait PaymentWorkflow {

  @workflowMethod
  def proceed(transaction: ProceedTransactionCommand): TransactionView

  @queryMethod
  def isFinished(): Boolean

  @signalMethod
  def confirmTransaction(command: ConfirmTransactionCommand): Unit
}
