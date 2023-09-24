package zio.temporal

import zio._
import io.temporal.common.WorkflowExecutionHistory
import scala.jdk.CollectionConverters._

/** Provides a wrapper with convenience methods over raw protobuf `History` object representing workflow history
  */
final class ZWorkflowExecutionHistory(val toJava: WorkflowExecutionHistory) {
  def workflowExecution =
    new ZWorkflowExecution(toJava.getWorkflowExecution)

  def events: List[ZHistoryEvent] =
    toJava.getEvents.asScala.view.map(new ZHistoryEvent(_)).toList

  def lastEvent: Option[ZHistoryEvent] =
    Option(toJava.getLastEvent).map(new ZHistoryEvent(_))

  override def toString: String =
    s"ZWorkflowExecutionHistory(" +
      s"workflowExecution=$workflowExecution, " +
      s"events=$events, " +
      s"lastEvent=$lastEvent" +
      s")"
}

object ZWorkflowExecutionHistory {

  /** @param jsonString
    *   history json (tctl format) to import and deserialize into History
    * @param workflowId
    *   workflow id to be used in [[ZWorkflowExecutionHistory.workflowExecution]]
    * @return
    *   WorkflowExecutionHistory
    */
  def fromJson(jsonString: String, workflowId: Option[String] = None): Task[ZWorkflowExecutionHistory] =
    ZIO.attemptBlocking {
      new ZWorkflowExecutionHistory(
        workflowId.fold(ifEmpty = WorkflowExecutionHistory.fromJson(jsonString))(
          WorkflowExecutionHistory.fromJson(jsonString, _)
        )
      )
    }
}
