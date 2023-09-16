package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.temporal.state._

// Different names just to ensure uniqueness
@activityInterface
trait ZioUntypedActivity {
  @activityMethod(name = "EchoUntyped")
  def echo(what: String): String
}

class ZioUntypedActivityImpl(implicit options: ZActivityRunOptions[Any]) extends ZioUntypedActivity {
  override def echo(what: String): String =
    ZActivity.run {
      ZIO
        .log(s"Echo message=$what")
        .as(s"Echoed $what")
    }
}

@workflowInterface
trait ZioWorkflowUntyped {
  @workflowMethod
  def echo(what: String): String

  @signalMethod
  def complete(): Unit
}

/** Untyped version of [[ZioWorkflow]] */
class ZioWorkflowUntypedImpl extends ZioWorkflowUntyped {
  private val state  = ZWorkflowState.empty[Unit]
  private val logger = ZWorkflow.makeLogger

  private val activity = ZWorkflow
    .newUntypedActivityStub(
      ZActivityOptions.withStartToCloseTimeout(5.seconds)
    )

  override def echo(what: String): String = {
    val msg = activity.execute[String]("EchoUntyped", what)

    logger.info("Waiting for completion...")
    ZWorkflow.awaitWhile(state.isEmpty)
    msg
  }

  override def complete(): Unit = {
    logger.info("Completion received!")
    state := ()
  }
}
