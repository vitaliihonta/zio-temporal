package ztemporal.fixture

import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import ztemporal.workflow.ZWorkflow

@WorkflowInterface
trait SignalWorkflow {

  @WorkflowMethod
  def echoServer(prefix: String): String

  @QueryMethod(name = "progress")
  def getProgress: Option[String]

  @SignalMethod
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
