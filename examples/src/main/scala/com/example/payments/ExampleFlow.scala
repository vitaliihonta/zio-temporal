package com.example.payments

import com.example.payments.workflows.PaymentWorkflow
import com.example.transactions._
import zio.{LogAnnotation => _, _}
import zio.logging.LogAnnotation
import zio.logging.logContext
import zio.temporal.proto.syntax._
import zio.temporal.signal._
import zio.temporal.workflow._

import java.util.UUID

object ExampleFlow {
  val make: URLayer[ZWorkflowClient, ExampleFlow] = ZLayer.fromFunction(new ExampleFlow(_))
}

class ExampleFlow(client: ZWorkflowClient) {

  private val transactionIdAnnotation = LogAnnotation[UUID](
    name = "transaction_id",
    combine = (_: UUID, r: UUID) => r,
    render = _.toString
  )

  def proceedPayment(): UIO[Unit] =
    Random.nextUUID.flatMap { transactionId =>
      val paymentFlow = for {
        _ <- logContext.update(
               _.annotate(
                 transactionIdAnnotation,
                 transactionId
               )
             )
        sender   <- Random.nextUUID
        receiver <- Random.nextUUID
        paymentWorkflow <- client
                             .newWorkflowStub[PaymentWorkflow]
                             .withTaskQueue("payments")
                             .withWorkflowId(transactionId.toString)
                             .build
        _            <- ZIO.logInfo("Going to trigger workflow")
        _            <- initiateTransaction(paymentWorkflow)(transactionId, sender, receiver)
        _            <- ZIO.logInfo("Trxn workflow started id")
        _            <- simulateUserActivity
        workflowStub <- client.newWorkflowStubProxy[PaymentWorkflow](workflowId = transactionId.toString)
        currentState <- checkStatus(workflowStub)(transactionId)
        _            <- ZIO.logInfo(s"Trxn status checked $currentState")
        _            <- simulateUserActivity
        _            <- ZIO.logInfo("Going to send confirmation")
        _            <- sendConfirmation(workflowStub)(transactionId)
        _            <- ZIO.logInfo("Confirmation sent!")
        _            <- simulateUserActivity
        _            <- (ZIO.sleep(100.millis) *> checkStatus(workflowStub)(transactionId)).repeatWhile(isNotFinished)
        _            <- ZIO.logInfo("End-up polling status, fetching the result")
        result       <- workflowStub.resultEither[TransactionError, TransactionView]
        _            <- ZIO.logInfo(s"Transaction finished result=$result")
      } yield ()

      paymentFlow.catchAll { error =>
        ZIO.logError(s"Error processing transaction: $error")
      }
    }

  private def initiateTransaction(
    paymentWorkflow: ZWorkflowStub.Of[PaymentWorkflow]
  )(transactionId:   UUID,
    sender:          UUID,
    receiver:        UUID
  ) =
    ZWorkflowStub.start(
      paymentWorkflow.proceed(
        ProceedTransactionCommand(
          id = transactionId.toProto,
          sender = sender.toProto,
          receiver = receiver.toProto,
          amount = BigDecimal(9000).toProto
        )
      )
    )

  private def checkStatus(workflowStub: ZWorkflowStub)(transactionId: UUID) =
    ZIO.logInfo("Checking transaction status...") *>
      workflowStub
        .query0((_: PaymentWorkflow).getStatus)
        .runEither

  private def sendConfirmation(
    workflowStub:  ZWorkflowStub.Proxy[PaymentWorkflow]
  )(transactionId: UUID
  ) =
    ZWorkflowStub.signal(
      workflowStub.confirmTransaction(
        // change confirmation code to see what happens
        ConfirmTransactionCommand(id = transactionId.toProto, confirmationCode = "42")
      )
    )

  private def isNotFinished(state: TransactionView): Boolean =
    state.status.isCreated || state.status.isInProgress

  private def simulateUserActivity: UIO[Unit] =
    ZIO.logInfo("User is thinking...") *>
      ZIO.sleep(1.second)
}
