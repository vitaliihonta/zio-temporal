package com.example.payments.impl

import com.example.payments.workflows.PaymentActivity
import com.example.payments.workflows.PaymentWorkflow
import com.example.transactions._
import zio._
import zio.temporal._
import zio.temporal.saga._
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow.ZWorkflow
import org.slf4j.LoggerFactory
import org.slf4j.MDC

case class TransactionState(transaction: TransactionView, confirmation: Option[ConfirmTransactionCommand])

class PaymentWorkflowImpl extends PaymentWorkflow {

  private lazy val logger = LoggerFactory.getLogger(getClass)
  MDC.put("transaction_id", ZWorkflow.info.workflowId)

  private val activity = ZWorkflow
    .newActivityStub[PaymentActivity]
    .withStartToCloseTimeout(5.seconds)
    .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1))
    .build

  private val state = ZWorkflowState.empty[TransactionState]

  override def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView] = {
    logger.info(s"Processing transaction=$transaction")
    state.setTo(initialState(transaction))
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
      failTransaction(error.message)
      logger.error(s"Transaction failed code=${error.code} message=${error.message}")
    }
    result
  }

  override def getStatus: Either[TransactionError, TransactionView] =
    state
      .toEither(TransactionError(code = 1, message = "Transaction not initialized"))
      .map(_.transaction)

  override def confirmTransaction(command: ConfirmTransactionCommand): Unit =
    state.updateWhen {
      case state if state.transaction.status.isInProgress =>
        state.copy(confirmation = Some(command))
    }

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
      confirmation = None
    )

  private def proceedTransaction(command: ProceedTransactionCommand): ZSaga[TransactionError, TransactionView] =
    ZSaga.make(activity.proceed(command))(
      compensate = cancelTransaction()
    )

  private def cancelTransaction(): Unit =
    state.toOption.foreach { state =>
      activity.cancelTransaction(CancelTransactionCommand(id = state.transaction.id))
    }

  private def handleConfirmation(): ZSaga[TransactionError, Unit] = {
    val currentState = state.snapshot
    if (currentState.transaction.status.isFailed)
      ZSaga.fail(TransactionError(code = -1, message = "Transaction failed"))
    else
      currentState.confirmation match {
        case Some(confirmation) => verifyConfirmation(confirmation)
        case None               => ZSaga.fail(TransactionError(code = 500, message = "Invalid transaction state"))
      }
  }

  private def verifyConfirmation(confirmation: ConfirmTransactionCommand): ZSaga[TransactionError, Unit] =
    ZSaga
      .make(activity.verifyConfirmation(confirmation))(compensate = cancelTransaction())
      .map(_ => finalizeTransaction())
      .unit

  private def failTransaction(description: String): ZSaga[Nothing, Unit] =
    ZSaga.succeed {
      state.updateWhen {
        case trxn: TransactionState if trxn.transaction.status.isCreated | trxn.transaction.status.isInProgress =>
          trxn.copy(transaction = trxn.transaction.copy(status = TransactionStatus.Failed, description = description))
      }
    }

  private def finalizeTransaction(): Unit =
    state.update(s =>
      s.copy(transaction = s.transaction.copy(status = TransactionStatus.Succeeded, description = "Processed"))
    )

  private def updateStateWith(transaction: TransactionView): ZSaga[Nothing, Unit] =
    ZSaga.succeed {
      state.update(_.copy(transaction = transaction))
    }

  private def waitForConfirmation(): ZSaga[Nothing, Unit] =
    ZSaga.succeed {
      ZWorkflow.awaitWhile(
        state.snapshotOf(_.transaction.status) == TransactionStatus.InProgress &&
          state.snapshotOf(_.confirmation).isEmpty
      )
    }
}
