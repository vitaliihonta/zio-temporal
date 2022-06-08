package ztemporal.fixture

import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod

@WorkflowInterface
trait SampleWorkflow {

  @WorkflowMethod
  def echo(str: String): String
}

class SampleWorkflowImpl() extends SampleWorkflow {
  override def echo(str: String): String = str
}
