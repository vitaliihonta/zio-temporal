package zio.temporal

import zio._
import io.temporal.workflow.WorkflowInfo
import scala.compat.java8.OptionConverters._
import scala.jdk.CollectionConverters._

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

  def continuedExecutionRunId: Option[String] = toJava.getContinuedExecutionRunId.asScala
  def parentWorkflowId: Option[String]        = toJava.getParentWorkflowId.asScala
  def parentRunId: Option[String]             = toJava.getParentRunId.asScala

  def workflowRunTimeout: Duration       = Duration.fromJava(toJava.getWorkflowRunTimeout)
  def workflowExecutionTimeout: Duration = Duration.fromJava(toJava.getWorkflowExecutionTimeout)

  def runStartedTimestampMillis: Long = toJava.getRunStartedTimestampMillis

  def searchAttributes: Map[String, String] =
    toJava.getSearchAttributes.getIndexedFieldsMap.asScala.map { case (k, v) => k -> v.getData.toStringUtf8 }.toMap
}
