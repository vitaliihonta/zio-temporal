package zio.temporal.fixture

import zio.temporal._

@workflow
trait SampleWorkflow {

  @workflowMethod
  def echo(str: String): String
}

class SampleWorkflowImpl() extends SampleWorkflow {
  override def echo(str: String): String = str
}
