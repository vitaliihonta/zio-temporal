package com.example.payments.impl

import com.example.payments.workflows.{InvalidConfirmationCodeError, PaymentActivity, PaymentWorkflow}
import com.example.transactions._
import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.state._
import zio.temporal.activity.{ZActivityOptions, ZActivityStub}
import org.slf4j.MDC
import zio.temporal.failure.{ActivityFailure, ApplicationFailure}

import scala.util.control.NoStackTrace

case class TransactionState(
  transaction: TransactionView,
  confirmation: Option[ConfirmTransactionCommand]) {}

private[impl] case object ConfirmationTimeout extends Exception("Confirmation timeout") with NoStackTrace

class PaymentWorkflowImpl extends PaymentWorkflow {

  private lazy val logger = ZWorkflow.makeLogger
  MDC.put("transaction_id", ZWorkflow.info.workflowId)

  private val activity: ZActivityStub.Of[PaymentActivity] = ZWorkflow
    .newActivityStub[PaymentActivity](
      ZActivityOptions
        .withStartToCloseTimeout(10.seconds)
        .withRetryOptions(
          ZRetryOptions.default
            .withMaximumAttempts(5)
            .withDoNotRetry(nameOf[InvalidConfirmationCodeError])
        )
    )

  private val state = ZWorkflowState.empty[TransactionState]

  override def proceed(transaction: ProceedTransactionCommand): TransactionView = {
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
    state.snapshot.transaction
  }

  override def isFinished(): Boolean =
    state.exists(s => s.transaction.status.isFailed || s.transaction.status.isSucceeded)

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
        description = "Created",
        sender = command.sender,
        receiver = command.receiver,
        amount = command.amount
      ),
      confirmation = None
    )

  private def proceedTransaction(command: ProceedTransactionCommand): ZSaga[TransactionView] =
    ZSaga.make(
      ZActivityStub.execute(
        activity.proceed(command)
      )
    )(
      compensate = cancelTransaction()
    )

  private def cancelTransaction(): Unit =
    state.toOption.foreach { state =>
      ZActivityStub.execute(
        activity.cancelTransaction(CancelTransactionCommand(id = state.transaction.id))
      )
    }

  private def handleConfirmation(): ZSaga[Unit] = {
    state.snapshotOf(_.confirmation) match {
      case Some(confirmation) => verifyConfirmation(confirmation)
      // should not be handled by Temporal
      case None => ZSaga.fail(new RuntimeException("Invalid transaction state, it shouldn't happen"))
    }
  }

  private def verifyConfirmation(confirmation: ConfirmTransactionCommand): ZSaga[Unit] =
    ZSaga
      .attempt(
        ZActivityStub.execute(
          activity.verifyConfirmation(confirmation)
        )
      )
      .as(finalizeTransaction())
      .unit

  private def failTransaction(error: Throwable): Unit =
    state.update { trxn =>
      val InvalidConfirmationCodeError = nameOf[InvalidConfirmationCodeError]
      val description = error match {
        case ActivityFailure.Cause(ApplicationFailure(InvalidConfirmationCodeError, _, _, _)) =>
          "Contact issuer bank"
        // Confirmation timed out
        case ConfirmationTimeout =>
          ConfirmationTimeout.getMessage
        case _ =>
          "Contact support"
      }
      trxn.copy(
        transaction = trxn.transaction.copy(status = TransactionStatus.Failed, description = description)
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

  private def waitForConfirmation(): ZSaga[Unit] = {
    val confirmed = ZWorkflow.awaitWhile(40.seconds /*Confirmation timeout*/ )(
      state.exists(state =>
        state.transaction.status == TransactionStatus.InProgress &&
          state.confirmation.isEmpty
      )
    )
    if (!confirmed) {
      ZSaga.fail(ConfirmationTimeout)
    } else {
      ZSaga.unit
    }
  }
}
