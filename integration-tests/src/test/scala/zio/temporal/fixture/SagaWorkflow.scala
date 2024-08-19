package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._

case class TransferError(msg: String) extends Exception(msg)
case class Done()

@activityInterface
trait TransferActivity {
  @throws[TransferError]
  def deposit(account: String, amount: BigDecimal): Done

  @throws[TransferError]
  def withdraw(account: String, amount: BigDecimal): Done
}

class TransferActivityImpl(
  depositFunc:  (String, BigDecimal) => IO[TransferError, Done],
  withdrawFunc: (String, BigDecimal) => IO[TransferError, Done]
)(implicit options: ZActivityRunOptions[Any])
    extends TransferActivity {

  override def deposit(account: String, amount: BigDecimal): Done = {
    ZActivity.run {
      ZIO.logInfo(s"Deposit account=$account amount=$amount") *>
        depositFunc(account, amount)
    }
  }

  override def withdraw(account: String, amount: BigDecimal): Done =
    ZActivity.run {
      ZIO.logInfo(s"withdraw account=$account amount=$amount") *>
        withdrawFunc(account, amount)
    }
}

case class TransferCommand(from: String, to: String, amount: BigDecimal)

@workflowInterface
trait SagaWorkflow {

  @workflowMethod
  def transfer(command: TransferCommand): BigDecimal
}

class SagaWorkflowImpl extends SagaWorkflow {

  private val activity = ZWorkflow.newActivityStub[TransferActivity](
    ZActivityOptions.withStartToCloseTimeout(5.seconds)
  )

  /** Using [[println]] here to see those logs while running [[WorkflowReplayerSpec]]
    */
  override def transfer(command: TransferCommand): BigDecimal = {
    println(s"Transfer command=$command")
    val saga = for {
      _ <- ZSaga.attempt(
             ZActivityStub.execute(
               activity.withdraw(command.from, command.amount)
             )
           )
      _ <- ZSaga.make(
             ZActivityStub.execute(
               activity.deposit(command.to, command.amount)
             )
           )(compensate = ZActivityStub.execute(activity.deposit(command.from, command.amount)))
    } yield command.amount

    saga.runOrThrow()
  }
}
