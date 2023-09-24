package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.temporal.state._

@workflowInterface
trait ZioLocalWorkflow {
  @workflowMethod
  def echo(what: String): String

  @signalMethod
  def complete(): Unit
}

class ZioLocalWorkflowImpl extends ZioLocalWorkflow {
  private val state  = ZWorkflowState.empty[Unit]
  private val logger = ZWorkflow.makeLogger

  private val activity = ZWorkflow
    .newLocalActivityStub[ZioActivity](
      ZLocalActivityOptions.withStartToCloseTimeout(5.seconds)
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
