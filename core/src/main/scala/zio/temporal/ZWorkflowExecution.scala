package zio.temporal

import io.temporal.api.common.v1.WorkflowExecution

/** Represents workflow execution information
  * @see
  *   [[WorkflowExecution]]
  */
final class ZWorkflowExecution(val toJava: WorkflowExecution) {
  def workflowId: String     = toJava.getWorkflowId
  def runId: String          = toJava.getRunId
  def isInitialized: Boolean = toJava.isInitialized
}
