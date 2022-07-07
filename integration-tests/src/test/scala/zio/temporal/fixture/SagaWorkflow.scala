package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.saga._
import zio.temporal.workflow._

case class Error(msg: String)
case class Done()

@activityInterface
trait TransferActivity {
  def deposit(account: String, amount: BigDecimal): Either[Error, Done]

  def withdraw(account: String, amount: BigDecimal): Either[Error, Done]
}

class TransferActivityImpl(
  depositFunc:  (String, BigDecimal) => Either[Error, Done],
  withdrawFunc: (String, BigDecimal) => Either[Error, Done])
    extends TransferActivity {

  override def deposit(account: String, amount: BigDecimal): Either[Error, Done] =
    depositFunc(account, amount)

  override def withdraw(account: String, amount: BigDecimal): Either[Error, Done] =
    withdrawFunc(account, amount)
}

case class TransferCommand(from: String, to: String, amount: BigDecimal)

@workflowInterface
trait SagaWorkflow {

  @workflowMethod
  def transfer(command: TransferCommand): Either[Error, BigDecimal]
}

class SagaWorkflowImpl extends SagaWorkflow {

  private val activity = ZWorkflow
    .newActivityStub[TransferActivity]
    .withStartToCloseTimeout(5.seconds)
    .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1))
    .build

  override def transfer(command: TransferCommand): Either[Error, BigDecimal] = {
    val saga = for {
      _ <- ZSaga.fromEither(activity.withdraw(command.from, command.amount))
      _ <- ZSaga.make(
             activity.deposit(command.to, command.amount)
           )(compensate = activity.deposit(command.from, command.amount))
    } yield command.amount

    saga.run()
  }
}
