package zio.temporal

import io.temporal.api.common.v1.Payload
import zio.*
import io.temporal.workflow.WorkflowInfo

import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*

/** Represents current workflow information
  * @see
  *   [[WorkflowInfo]]
  */
final class ZWorkflowInfo private[zio] (val toJava: WorkflowInfo) {
  def namespace: String    = toJava.getNamespace
  def workflowId: String   = toJava.getWorkflowId
  def runId: String        = toJava.getRunId
  def workflowType: String = toJava.getWorkflowType
  def taskQueue: String    = toJava.getTaskQueue
  def attempt: Int         = toJava.getAttempt

  def continuedExecutionRunId: Option[String] = toJava.getContinuedExecutionRunId.toScala
  def parentWorkflowId: Option[String]        = toJava.getParentWorkflowId.toScala
  def parentRunId: Option[String]             = toJava.getParentRunId.toScala

  def workflowRunTimeout: Duration       = Duration.fromJava(toJava.getWorkflowRunTimeout)
  def workflowExecutionTimeout: Duration = Duration.fromJava(toJava.getWorkflowExecutionTimeout)

  def runStartedTimestampMillis: Long = toJava.getRunStartedTimestampMillis

  def searchAttributes: Map[String, Payload] = {
    val attrsOpt = Option(toJava.getSearchAttributes) // nullable
    attrsOpt.map(_.getIndexedFieldsMap.asScala.toMap).getOrElse(Map.empty)
  }

  override def toString: String = toJava.toString
    .replace("WorkflowInfo", "ZWorkflowInfo")
    .replace("{", "(")
    .replace("}", ")")
}
