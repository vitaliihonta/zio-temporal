package com.example.payments.workflows

import com.example.transactions._
import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
trait PaymentWorkflow {

  @WorkflowMethod
  def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView]

  @QueryMethod
  def getStatus: Either[TransactionError, TransactionView]

  @SignalMethod
  def confirmTransaction(command: ConfirmTransactionCommand): Unit
}
