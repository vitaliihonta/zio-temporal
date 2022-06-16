package ztemporal

import io.temporal.api.common.v1.WorkflowExecution

/** Represents workflow execution information
  * @see
  *   [[WorkflowExecution]]
  */
class ZWorkflowExecution private[ztemporal] (val toJava: WorkflowExecution) extends AnyVal {
  def workflowId: String     = toJava.getWorkflowId
  def runId: String          = toJava.getRunId
  def isInitialized: Boolean = toJava.isInitialized
}
