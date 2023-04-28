package zio.temporal.fixture

import zio.temporal._

@workflowInterface
trait SampleWorkflow {

  @workflowMethod
  def echo(str: String): String
}

class SampleWorkflowImpl() extends SampleWorkflow {
  override def echo(str: String): String = str
}

@workflowInterface
trait SampleNamedWorkflow {

  @workflowMethod(name = "DoEchoWorkflow")
  def echo(str: String): String
}

class SampleNamedWorkflowImpl() extends SampleNamedWorkflow {
  override def echo(str: String): String = str
}
