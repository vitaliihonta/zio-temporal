package zio.temporal.schedules

import io.temporal.client.schedules.{
  ScheduleActionExecution,
  ScheduleActionExecutionStartWorkflow,
  ScheduleActionResult
}
import org.slf4j.LoggerFactory

import java.time.Instant

final class ZScheduleActionResult private[zio] (val toJava: ScheduleActionResult) {
  def scheduledAt: Instant = toJava.getScheduledAt

  def startedAt: Instant = toJava.getStartedAt

  def action: ZScheduleActionExecution = ZScheduleActionExecution(toJava.getAction)

  override def toString: String = {
    "ZScheduleActionResult(" +
      s"scheduledAt=$scheduledAt" +
      s", startedAt=$startedAt" +
      s", action=$action" +
      ")"
  }
}

sealed trait ZScheduleActionExecution {
  def toJava: ScheduleActionExecution
}

object ZScheduleActionExecution {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  def apply(value: ScheduleActionExecution): ZScheduleActionExecution = value match {
    case value: ScheduleActionExecutionStartWorkflow => new StartWorkflow(value)
    case _ =>
      logger.warn(
        s"Unknown implementation of io.temporal.client.schedules.ScheduleActionExecution found: class=${value.getClass} value=$value"
      )
      Unknown(value)
  }

  final case class StartWorkflow private[zio] (toJava: ScheduleActionExecutionStartWorkflow)
      extends ZScheduleActionExecution {

    def workflowId: String = toJava.getWorkflowId

    def firstExecutionRunId: String = toJava.getFirstExecutionRunId

    override def toString: String =
      s"ZScheduleActionExecution.StartWorkflow(" +
        s"workflowId=${workflowId}" +
        s", firstExecutionRunId=${firstExecutionRunId}" +
        s")"
  }

  final case class Unknown private[zio] (toJava: ScheduleActionExecution) extends ZScheduleActionExecution
}
