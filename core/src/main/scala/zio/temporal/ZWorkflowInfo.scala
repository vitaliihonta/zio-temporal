package zio.temporal

import zio._
import io.temporal.workflow.WorkflowInfo
import scala.jdk.OptionConverters._

/** Represents current workflow information
  * @see
  *   [[WorkflowInfo]]
  */
final class ZWorkflowInfo private[zio] (val toJava: WorkflowInfo) {

  /** @return
    *   Workflow Namespace
    */
  def namespace: String =
    toJava.getNamespace

  /** @return
    *   Workflow ID
    */
  def workflowId: String =
    toJava.getWorkflowId

  /** Note: RunId is unique identifier of one workflow code execution. Reset changes RunId.
    *
    * @return
    *   Workflow Run ID that is handled by the current workflow code execution.
    * @see
    *   [[originalExecutionRunId]] for RunId variation that is resistant to Resets
    * @see
    *   [[firstExecutionRunId]] for the very first RunId that is preserved along the whole Workflow Execution chain,
    *   including ContinueAsNew, Retry, Cron and Reset.
    */
  def runId: String =
    toJava.getRunId

  /** @return
    *   Workflow Type
    */
  def workflowType: String =
    toJava.getWorkflowType

  /** @return
    *   Workflow Task Queue name
    */
  def taskQueue: String =
    toJava.getTaskQueue

  /** @return
    *   Workflow retry attempt handled by this Workflow code execution. Starts on "1".
    */
  def attempt: Int =
    toJava.getAttempt

  /** @return
    *   Run ID of the previous Workflow Run which continued-as-new or retried or cron-scheduled into the current
    *   Workflow Run.
    */
  def continuedExecutionRunId: Option[String] =
    toJava.getContinuedExecutionRunId.toScala

  /** @return
    *   Workflow ID of the parent Workflow
    */
  def parentWorkflowId: Option[String] =
    toJava.getParentWorkflowId.toScala

  /** @return
    *   Run ID of the parent Workflow
    */
  def parentRunId: Option[String] =
    toJava.getParentRunId.toScala

  /** Note: This value is NOT preserved by continue-as-new, retries or cron Runs. They are separate Runs of one Workflow
    * Execution Chain.
    *
    * @return
    *   original RunId of the current Workflow Run. This value is preserved during Reset which changes RunID.
    * @see
    *   [[firstExecutionRunId]] for the very first RunId that is preserved along the whole Workflow Execution chain,
    *   including ContinueAsNew, Retry, Cron and Reset.
    */
  def originalExecutionRunId: String =
    toJava.getOriginalExecutionRunId

  /** @return
    *   The very first original RunId of the current Workflow Execution preserved along the chain of ContinueAsNew,
    *   Retry, Cron and Reset. Identifies the whole Runs chain of Workflow Execution.
    */
  def firstExecutionRunId: String =
    toJava.getFirstExecutionRunId

  /** @return
    *   Workflow cron schedule
    */
  def cronSchedule: Option[String] =
    Option(toJava.getCronSchedule)

  /** @return
    *   length of Workflow history up until the current moment of execution. This value changes during the lifetime of a
    *   Workflow Execution. You may use this information to decide when to use `continueAsNew`.
    */
  def getHistoryLength: Long =
    Option(toJava.getHistoryLength).getOrElse(0L)

  /** @return
    *   Timeout for a Workflow Run specified during Workflow start
    */
  def workflowRunTimeout: Duration =
    Duration.fromJava(toJava.getWorkflowRunTimeout)

  /** @return
    *   Timeout for the Workflow Execution specified during Workflow start
    */
  def workflowExecutionTimeout: Duration =
    Duration.fromJava(toJava.getWorkflowExecutionTimeout)

  /** The time workflow run has started. Note that this time can be different from the time workflow function started
    * actual execution.
    */
  def runStartedTimestampMillis: Long =
    toJava.getRunStartedTimestampMillis

  override def toString: String = {
    toJava.toString
      .replace("WorkflowInfo", "ZWorkflowInfo")
      .replace("{", "(")
      .replace("}", ")")
  }
}
