package com.example.payments

import com.example.payments.workflows.PaymentWorkflow
import com.example.transactions._
import logstage.LogIO
import ztemporal.workflow._
import ztemporal.signal._
import ztemporal.proto.syntax._
import zio._
import zio.duration._
import java.util.UUID

class ExampleFlow(client: ZWorkflowClient, rootLogger: LogIO[UIO]) {

  def proceedPayment(): URIO[ZEnv, Unit] =
    random.nextUUID.flatMap { transactionId =>
      val logger = rootLogger("transaction_id" -> transactionId)
      val paymentFlow = for {
        sender   <- random.nextUUID
        receiver <- random.nextUUID
        paymentWorkflow <- client
                             .newWorkflowStub[PaymentWorkflow]
                             .withTaskQueue("payments")
                             .withWorkflowId(transactionId.toString)
                             .build
        _            <- logger.info("Going to trigger workflow")
        _            <- initiateTransaction(paymentWorkflow)(transactionId, sender, receiver)
        _            <- logger.info("Trxn workflow started id")
        _            <- simulateUserActivity(logger)
        workflowStub <- client.newUntypedWorkflowStub(workflowId = transactionId.toString)
        currentState <- checkStatus(workflowStub, logger)(transactionId)
        _            <- logger.info(s"Trxn status checked $currentState")
        _            <- simulateUserActivity(logger)
        _            <- logger.info("Going to send confirmation")
        _            <- sendConfirmation(workflowStub, paymentWorkflow)(transactionId)
        _            <- logger.info("Confirmation sent!")
        _            <- simulateUserActivity(logger)
        _            <- (clock.sleep(100.millis) *> checkStatus(workflowStub, logger)(transactionId)).repeatWhile(isNotFinished)
        _            <- logger.info("End-up polling status, fetching the result")
        result       <- workflowStub.resultEither[TransactionError, TransactionView]
        _            <- logger.info(s"Transaction finished $result")
      } yield ()

      paymentFlow.catchAll { error =>
        logger.error(s"Error processing transaction: $error")
      }
    }

  private def initiateTransaction(
    paymentWorkflow: ZWorkflowStub.Of[PaymentWorkflow]
  )(transactionId:   UUID,
    sender:          UUID,
    receiver:        UUID
  ) =
    (paymentWorkflow.proceed _).start(
      ProceedTransactionCommand(
        id = transactionId.toProto,
        sender = sender.toProto,
        receiver = receiver.toProto,
        amount = BigDecimal(9000).toProto
      )
    )

  private def checkStatus(workflowStub: ZWorkflowStub, logger: LogIO[UIO])(transactionId: UUID) =
    logger.info("Checking transaction status...") *>
      workflowStub
        .query0((_: PaymentWorkflow).getStatus)
        .runEither

  private def sendConfirmation(
    workflowStub:    ZWorkflowStub,
    paymentWorkflow: ZWorkflowStub.Of[PaymentWorkflow]
  )(transactionId:   UUID
  ) =
    workflowStub.signal(
      ZSignal.signal(paymentWorkflow.confirmTransaction _)
    )(
      zinput(
        ConfirmTransactionCommand(id = transactionId.toProto, confirmationCode = "42")
      )
    ) // change confirmation code to see what happens

  private def isNotFinished(state: TransactionView): Boolean =
    state.status.isCreated || state.status.isInProgress

  private def simulateUserActivity(logger: LogIO[UIO]): URIO[clock.Clock, Unit] =
    logger.info("User is thinking...") *>
      zio.clock.sleep(1.second)
}
