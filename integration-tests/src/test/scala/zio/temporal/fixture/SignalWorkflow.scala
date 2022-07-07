package zio.temporal.fixture

import zio.temporal._
import zio.temporal.workflow.ZWorkflow

@workflowInterface
trait SignalWorkflow {

  @workflowMethod
  def echoServer(prefix: String): String

  @queryMethod(name = "progress")
  def getProgress: Option[String]

  @signalMethod
  def echo(value: String): Unit
}

class SignalWorkflowImpl extends SignalWorkflow {
  private var message: Option[String] = None

  override def echoServer(prefix: String): String = {
    ZWorkflow.awaitWhile(message.isEmpty)
    s"$prefix ${message.get}"
  }

  override def echo(value: String): Unit = {
    println(s"echo($value)")
    message = Some(value)
  }

  override def getProgress: Option[String] =
    message
}
