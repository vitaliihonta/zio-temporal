package zio.temporal.fixture

import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*
import zio.temporal.state.*

@activityInterface
trait ZioActivity {
  def echo(what: String): String
}

class ZioActivityImpl(implicit options: ZActivityOptions[Any]) extends ZioActivity {
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
  private val state = ZWorkflowState.empty[Unit]

  private val activity = ZWorkflow
    .newActivityStub[ZioActivity]
    .withStartToCloseTimeout(5.seconds)
    .build

  override def echo(what: String): String = {
    val msg = activity.echo(what)
    println("Waiting for completion...")
    ZWorkflow.awaitWhile(state.isEmpty)
    msg
  }

  override def complete(): Unit =
    state := ()
}
