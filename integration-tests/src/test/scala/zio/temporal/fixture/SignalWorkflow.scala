package zio.temporal.fixture

import zio.temporal._
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow.ZWorkflow

@workflowInterface
trait SignalWorkflow {

  @workflowMethod
  def echoServer(prefix: String): String

  @queryMethod(name = "progress")
  def getProgress(default: Option[String]): Option[String]

  @signalMethod
  def echo(value: String): Unit
}

class SignalWorkflowImpl extends SignalWorkflow {
  private val message = ZWorkflowState.empty[String]

  override def echoServer(prefix: String): String = {
    ZWorkflow.awaitWhile(message.isEmpty)
    s"$prefix ${message.snapshot}"
  }

  override def echo(value: String): Unit = {
    println(s"echo($value)")
    message := value
  }

  override def getProgress(default: Option[String]): Option[String] = {
    println(s"Getting progress default=$default...")
    message.toOption.orElse(default)
  }
}
