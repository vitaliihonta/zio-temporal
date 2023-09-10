package zio.temporal.fixture

import zio.temporal._
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow.ZWorkflow

@workflowInterface
trait RetryWorkflow {

  @workflowMethod
  def withRetry(attempts: Int): String

}

class RetryWorkflowImpl extends RetryWorkflow { self =>

  private val state = ZWorkflowState.make(true)

  override def withRetry(attempts: Int): String = {
    ZWorkflow.retry(ZRetryOptions.default.withMaximumAttempts(attempts)) {
      if (state =:= true) {
        state := false
        throw new Exception("FAIL")
      } else "OK"
    }
  }

}
