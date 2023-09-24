package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.temporal.state._

@activityInterface
trait ZioActivity {
  @activityMethod(name = "Echo")
  def echo(what: String): String
}

class ZioActivityImpl(implicit options: ZActivityRunOptions[Any]) extends ZioActivity {
  override def echo(what: String): String =
    ZActivity.run {
      ZIO
        .log(s"Echo message=$what")
        .as(s"Echoed $what")
    }
}

@workflowInterface
trait ZioWorkflow {
  @workflowMethod
  def echo(what: String): String

  @signalMethod
  def complete(): Unit
}

class ZioWorkflowImpl extends ZioWorkflow {
  private val state  = ZWorkflowState.empty[Unit]
  private val logger = ZWorkflow.makeLogger

  private val activity = ZWorkflow
    .newActivityStub[ZioActivity](
      ZActivityOptions.withStartToCloseTimeout(5.seconds)
    )

  override def echo(what: String): String = {
    val msg = ZActivityStub.execute(
      activity.echo(what)
    )
    logger.info("Waiting for completion...")
    ZWorkflow.awaitWhile(state.isEmpty)
    msg
  }

  override def complete(): Unit = {
    logger.info("Completion received!")
    state := ()
  }
}
