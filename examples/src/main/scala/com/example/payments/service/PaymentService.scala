package com.example.payments.service

import com.example.payments.workflows.PaymentWorkflow
import com.example.transactions._
import java.util.UUID
import zio._
import zio.logging.LogAnnotation
import zio.logging.logContext
import zio.temporal._
import zio.temporal.protobuf.syntax._
import zio.temporal.workflow._

case class PaymentError(details: String)

case class Transaction(
  id:          UUID,
  status:      TransactionStatus,
  description: String)

object Transaction {
  def fromView(view: TransactionView): Transaction =
    Transaction(view.id.fromProto[UUID], view.status, view.description)
}

trait PaymentService {
  def createPayment(sender: UUID, receiver: UUID, amount: BigDecimal): IO[PaymentError, UUID]

  def getState(transactionId: UUID): IO[PaymentError, Option[Transaction]]

  def confirmPayment(transactionId: UUID, confirmationCode: String): IO[PaymentError, Unit]
}

object PaymentService {
  val make: URLayer[ZWorkflowClient, PaymentService] = ZLayer.fromFunction(new TemporalPaymentService(_))
}

class TemporalPaymentService(workflowClient: ZWorkflowClient) extends PaymentService {
  override def createPayment(sender: UUID, receiver: UUID, amount: BigDecimal): IO[PaymentError, UUID] =
    withErrorHandling {
      for {
        transactionId <- ZIO.randomWith(_.nextUUID)
        _             <- updateLogContext(transactionId)
        paymentWorkflow <- workflowClient
                             .newWorkflowStub[PaymentWorkflow]
                             .withTaskQueue("payments")
                             .withWorkflowId(transactionId.toString)
                             .withWorkflowExecutionTimeout(6.minutes)
                             // Avoid setting runTimeout < timeout in ZWorkflow.awaitWhile
                             // Otherwise, you'll get a creepy error
                             .withWorkflowRunTimeout(1.minute)
                             .withRetryOptions(
                               ZRetryOptions.default.withMaximumAttempts(5)
                             )
                             .build
        _ <- ZIO.logInfo("Going to trigger workflow")
        _ <- ZWorkflowStub.start(
               paymentWorkflow.proceed(
                 ProceedTransactionCommand(
                   id = transactionId,
                   sender = sender,
                   receiver = receiver,
                   amount = BigDecimal(9000)
                 )
               )
             )
      } yield transactionId
    }

  override def getState(transactionId: UUID): IO[PaymentError, Option[Transaction]] =
    withErrorHandling {
      for {
        _            <- updateLogContext(transactionId)
        workflowStub <- workflowClient.newWorkflowStub[PaymentWorkflow](workflowId = transactionId.toString)
        _            <- ZIO.logInfo("Checking if transaction is finished...")
        maybeTransaction <- ZIO.whenZIO(
                              ZWorkflowStub.query(
                                workflowStub.isFinished()
                              )
                            ) {
                              workflowStub.result[TransactionView]
                            }
        // Another option is to wait for result with a timeout
        // maybeTransaction <- workflowStub.result[TransactionView](2.seconds)
      } yield maybeTransaction.map(Transaction.fromView)
    }

  override def confirmPayment(transactionId: UUID, confirmationCode: String): IO[PaymentError, Unit] =
    withErrorHandling {
      for {
        _            <- updateLogContext(transactionId)
        workflowStub <- workflowClient.newWorkflowStub[PaymentWorkflow](workflowId = transactionId.toString)
        _            <- ZIO.logInfo("Going to send confirmation")
        isFinished <- ZWorkflowStub.query(
                        workflowStub.isFinished()
                      )
        _ <- ZIO.unless(isFinished) {
               ZWorkflowStub.signal(
                 workflowStub.confirmTransaction(
                   ConfirmTransactionCommand(id = transactionId, confirmationCode)
                 )
               ) *> ZIO.logInfo("Confirmation sent!")
             }
      } yield ()
    }

  private def withErrorHandling[R, A](thunk: TemporalRIO[R, A]): ZIO[R, PaymentError, A] =
    thunk.mapError { workflowException =>
      PaymentError(workflowException.toString)
    }

  private def updateLogContext(transactionId: UUID): UIO[Unit] =
    logContext.update(
      _.annotate(
        transactionIdAnnotation,
        transactionId
      )
    )

  private def transactionIdAnnotation = LogAnnotation[UUID](
    name = "transaction_id",
    combine = (_: UUID, r: UUID) => r,
    render = _.toString
  )
}
