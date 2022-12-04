package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.saga._
import zio.temporal.workflow._

@activityInterface
trait TransferExceptionalActivity {
  def deposit(account: String, amount: BigDecimal): Done

  def withdraw(account: String, amount: BigDecimal): Done
}

class TransferExceptionalActivityImpl(
  depositFunc:  (String, BigDecimal) => Done,
  withdrawFunc: (String, BigDecimal) => Done)
    extends TransferExceptionalActivity {

  override def deposit(account: String, amount: BigDecimal): Done =
    depositFunc(account, amount)

  override def withdraw(account: String, amount: BigDecimal): Done =
    withdrawFunc(account, amount)
}

@workflowInterface
trait SagaExceptionalWorkflow {

  @workflowMethod
  def transfer(command: TransferCommand): BigDecimal
}

class SagaExceptionalWorkflowImpl extends SagaExceptionalWorkflow {

  private val activity = ZWorkflow
    .newActivityStub[TransferExceptionalActivity]
    .withStartToCloseTimeout(5.seconds)
    .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1))
    .build

  override def transfer(command: TransferCommand): BigDecimal = {
    val saga = for {
      _ <- ZSaga.attempt(activity.withdraw(command.from, command.amount))
      _ <- ZSaga.makeAttempt(
             activity.deposit(command.to, command.amount)
           )(compensate = activity.deposit(command.from, command.amount))
    } yield command.amount

    saga.runOrThrow()
  }
}
