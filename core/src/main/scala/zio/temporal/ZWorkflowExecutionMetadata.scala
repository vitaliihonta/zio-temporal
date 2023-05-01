package zio.temporal

import io.temporal.api.common.v1.Payload
import io.temporal.api.enums.v1.WorkflowExecutionStatus
import io.temporal.client.WorkflowExecutionMetadata
import scala.jdk.CollectionConverters._
import java.time.Instant

final class ZWorkflowExecutionMetadata(val toJava: WorkflowExecutionMetadata) {

  def execution: ZWorkflowExecution = new ZWorkflowExecution(toJava.getExecution)

  def workflowType: String = toJava.getWorkflowType

  def taskQueue: String = toJava.getTaskQueue

  def startTime: Instant = toJava.getStartTime

  def executionTime: Instant = toJava.getExecutionTime

  def closeTime: Option[Instant] = Option(toJava.getCloseTime)

  def status: WorkflowExecutionStatus = toJava.getStatus

  def parentExecution: Option[ZWorkflowExecution] =
    Option(toJava.getParentExecution).map(new ZWorkflowExecution(_))

  def searchAttributes: Map[String, Payload] =
    toJava.getWorkflowExecutionInfo.getSearchAttributes.getIndexedFieldsMap.asScala.toMap

  override def toString: String =
    s"ZWorkflowExecutionMetadata(execution=$execution, " +
      s"workflowType=$workflowType, taskQueue=$taskQueue, " +
      s"startTime=$startTime, executionTime=$executionTime, closeTime=$closeTime, " +
      s"status=$status, parentExecution=$parentExecution" +
      s")"
}
