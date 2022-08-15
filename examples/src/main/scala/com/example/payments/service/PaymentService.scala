package com.example.payments.service

import com.example.payments.workflows.PaymentWorkflow
import com.example.transactions._
import java.util.UUID
import zio._
import zio.logging.LogAnnotation
import zio.logging.logContext
import zio.temporal._
import zio.temporal.protobuf.syntax._
import zio.temporal.workflow.ZWorkflowClient
import zio.temporal.workflow.ZWorkflowStub

case class PaymentError(details: String)

case class Transaction(
  id:     UUID,
  status: TransactionStatus) {

  def isFinished: Boolean = status.isSucceeded || status.isFailed
}

object Transaction {
  def fromView(view: TransactionView): Transaction =
    Transaction(view.id.fromProto[UUID], view.status)
}

trait PaymentService {
  def createPayment(sender: UUID, receiver: UUID, amount: BigDecimal): IO[PaymentError, UUID]

  def getStatus(transactionId: UUID): IO[PaymentError, Transaction]

  def confirmPayment(transactionId: UUID, confirmationCode: String): IO[PaymentError, Unit]
}

object PaymentService {
  val make: URLayer[ZWorkflowClient, PaymentService] = ZLayer.fromFunction(new TemporalPaymentService(_))
}

class TemporalPaymentService(client: ZWorkflowClient) extends PaymentService {
  override def createPayment(sender: UUID, receiver: UUID, amount: BigDecimal): IO[PaymentError, UUID] =
    withErrorHandling {
      Random.nextUUID.flatMap { transactionId =>
        for {
          _ <- updateLogContext(transactionId)
          paymentWorkflow <- client
                               .newWorkflowStub[PaymentWorkflow]
                               .withTaskQueue("payments")
                               .withWorkflowId(transactionId.toString)
                               .withWorkflowExecutionTimeout(5.minutes)
                               .withWorkflowRunTimeout(10.seconds)
                               .withRetryOptions(
                                 ZRetryOptions.default.withMaximumAttempts(5)
                               )
                               .build
          _ <- ZIO.logInfo("Going to trigger workflow")
          _ <- ZWorkflowStub.start(
                 paymentWorkflow.proceed(
                   ProceedTransactionCommand(
                     id = transactionId.toProto,
                     sender = sender.toProto,
                     receiver = receiver.toProto,
                     amount = BigDecimal(9000).toProto
                   )
                 )
               )
        } yield transactionId
      }
    }

  override def getStatus(transactionId: UUID): IO[PaymentError, Transaction] =
    withErrorHandling {
      for {
        _            <- updateLogContext(transactionId)
        workflowStub <- client.newWorkflowStubProxy[PaymentWorkflow](workflowId = transactionId.toString)
        _            <- ZIO.logInfo("Checking transaction status...")
        view <- ZWorkflowStub.query(
                  workflowStub.getStatus
                )
        trxn = Transaction.fromView(view)
        result <- if (!trxn.isFinished) ZIO.succeed(trxn)
                  else
                    workflowStub
                      .resultEither[TransactionError, TransactionView]
                      .map(Transaction.fromView)
      } yield result
    }

  override def confirmPayment(transactionId: UUID, confirmationCode: String): IO[PaymentError, Unit] =
    withErrorHandling {
      for {
        _            <- updateLogContext(transactionId)
        workflowStub <- client.newWorkflowStubProxy[PaymentWorkflow](workflowId = transactionId.toString)
        _            <- ZIO.logInfo("Going to send confirmation")
        status <- ZWorkflowStub.query(
                    workflowStub.getStatus
                  )
        _ <- ZIO.when(status.status.isFailed) {
               ZIO.fail(TemporalError(s"Cannot confirm transaction, it's already failed: ${status.description}"))
             }
        _ <- ZWorkflowStub.signal(
               workflowStub.confirmTransaction(
                 ConfirmTransactionCommand(id = transactionId.toProto, confirmationCode)
               )
             )
        _ <- ZIO.logInfo("Confirmation sent!")
      } yield ()
    }

  private def withErrorHandling[R, E, A](thunk: ZIO[R, TemporalError[E], A]): ZIO[R, PaymentError, A] =
    thunk.mapError { temporalError =>
      PaymentError(temporalError.message)
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
