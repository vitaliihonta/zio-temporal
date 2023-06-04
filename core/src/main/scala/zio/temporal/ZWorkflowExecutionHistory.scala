package zio.temporal

import io.temporal.common.WorkflowExecutionHistory
import scala.jdk.CollectionConverters.*

final class ZWorkflowExecutionHistory(val toJava: WorkflowExecutionHistory) {
  def workflowExecution: ZWorkflowExecution =
    new ZWorkflowExecution(toJava.getWorkflowExecution)

  def events: List[ZHistoryEvent] =
    toJava.getEvents.asScala.view.map(new ZHistoryEvent(_)).toList

  def lastEvent: Option[ZHistoryEvent] =
    Option(toJava.getLastEvent).map(new ZHistoryEvent(_))

  override def toString: String =
    s"ZWorkflowExecutionHistory(workflowExecution=$workflowExecution, " +
      s"events=$events, " +
      s"lastEvent=$lastEvent" +
      s")"
}
