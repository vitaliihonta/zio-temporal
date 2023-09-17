package zio.temporal.fixture

import zio.temporal._
import zio.temporal.workflow.ZWorkflow

@workflowInterface
trait MemoWorkflow {

  @workflowMethod
  def withMemo(key: String): Option[String]

}

class MemoWorkflowImpl extends MemoWorkflow { self =>

  override def withMemo(key: String): Option[String] = {
    ZWorkflow.getMemo[String](key)
  }

}
