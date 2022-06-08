package ztemporal.fixture

import io.temporal.activity.ActivityInterface
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import ztemporal.ZRetryOptions
import ztemporal.saga._
import ztemporal.workflow._
import scala.concurrent.duration._

case class Error(msg: String)
case class Done()

@ActivityInterface
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

@WorkflowInterface
trait SagaWorkflow {

  @WorkflowMethod
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
