package com.example.payments.impl

import com.example.payments.workflows.{InvalidConfirmationCodeError, PaymentActivity, PaymentWorkflow}
import com.example.transactions.*
import zio.*
import zio.temporal.*
import zio.temporal.saga.*
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow.ZWorkflow
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import zio.temporal.failure.{ActivityFailure, ApplicationFailure}

case class TransactionState(
  transaction:  TransactionView,
  confirmation: Option[ConfirmTransactionCommand],
  error:        Option[Throwable]) {

  def result: Either[TransactionError, TransactionView] = {
    val InvalidConfirmationCodeError = nameOf[InvalidConfirmationCodeError]
    error.toLeft(transaction).left.map {
      case ActivityFailure.Cause(ApplicationFailure(InvalidConfirmationCodeError, _, _, _)) =>
        TransactionError(code = 1, message = "Contact issuer bank")
      case _ =>
        TransactionError(code = 2, message = "Contact support")
    }
  }
}

class PaymentWorkflowImpl extends PaymentWorkflow {

  private lazy val logger = LoggerFactory.getLogger(getClass)
  MDC.put("transaction_id", ZWorkflow.info.workflowId)

  private val activity = ZWorkflow
    .newActivityStub[PaymentActivity]
    .withStartToCloseTimeout(10.seconds)
    .withRetryOptions(
      ZRetryOptions.default
        .withMaximumAttempts(3)
        .withDoNotRetry(nameOf[InvalidConfirmationCodeError])
    )
    .build

  private val state = ZWorkflowState.empty[TransactionState]

  override def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView] = {
    logger.info(s"Processing transaction=$transaction")
    state := initialState(transaction)
    val saga = for {
      created <- proceedTransaction(transaction)
      _ = logger.info(s"Initiated transaction=$created")
      _ <- updateStateWith(created)
      _ = logger.info("Waiting for confirmation")
      _ <- waitForConfirmation()
      _ = logger.info("Handling confirmation")
      _ <- handleConfirmation()
      _ = logger.info("Transaction processed successfully")
    } yield state.snapshot.transaction

    val result = saga.run()
    result.left.foreach { error =>
      failTransaction(error)
      logger.error(s"Transaction failed", error)
    }
    getStatusImpl
  }

  override def getStatus: Either[TransactionError, TransactionView] =
    getStatusImpl

  override def confirmTransaction(command: ConfirmTransactionCommand): Unit =
    state.updateWhen {
      case state if state.transaction.status.isInProgress =>
        state.copy(confirmation = Some(command))
    }

  private def getStatusImpl: Either[TransactionError, TransactionView] =
    state
      .toEither(TransactionError(code = 1, message = "Transaction not initialized"))
      .flatMap(_.result)

  private def initialState(command: ProceedTransactionCommand): TransactionState =
    TransactionState(
      transaction = TransactionView(
        id = command.id,
        status = TransactionStatus.Created,
        description = "created",
        sender = command.sender,
        receiver = command.receiver,
        amount = command.amount
      ),
      confirmation = None,
      error = None
    )

  private def proceedTransaction(command: ProceedTransactionCommand): ZSaga[TransactionView] =
    ZSaga.make(activity.proceed(command))(
      compensate = cancelTransaction()
    )

  private def cancelTransaction(): Unit =
    state.toOption.foreach { state =>
      activity.cancelTransaction(CancelTransactionCommand(id = state.transaction.id))
    }

  private def handleConfirmation(): ZSaga[Unit] = {
    state.snapshotOf(_.confirmation) match {
      case Some(confirmation) => verifyConfirmation(confirmation)
      case None               => ZSaga.fail(new Exception("Invalid transaction state, it shouldn't happen"))
    }
  }

  private def verifyConfirmation(confirmation: ConfirmTransactionCommand): ZSaga[Unit] =
    ZSaga
      .make(activity.verifyConfirmation(confirmation))(compensate = cancelTransaction())
      .as(finalizeTransaction())
      .unit

  private def failTransaction(error: Throwable): Unit =
    state.updateWhen {
      case trxn: TransactionState if trxn.transaction.status.isCreated | trxn.transaction.status.isInProgress =>
        trxn.copy(
          transaction = trxn.transaction.copy(status = TransactionStatus.Failed, description = "Transaction failed"),
          error = Some(error)
        )
    }

  private def finalizeTransaction(): Unit =
    state.update(s =>
      s.copy(transaction = s.transaction.copy(status = TransactionStatus.Succeeded, description = "Processed"))
    )

  private def updateStateWith(transaction: TransactionView): ZSaga[Unit] =
    ZSaga.succeed {
      state.update(_.copy(transaction = transaction))
    }

  private def waitForConfirmation(): ZSaga[Unit] =
    ZSaga.succeed {
      ZWorkflow.awaitWhile(
        state.exists(state =>
          state.transaction.status == TransactionStatus.InProgress &&
            state.confirmation.isEmpty
        )
      )
    }
}
