package zio.temporal.schedules

import io.temporal.client.schedules._
import io.temporal.client.WorkflowOptions
import io.temporal.common.converter.EncodedValues
import io.temporal.common.interceptors.Header
import zio.temporal.internalApi
import org.slf4j.LoggerFactory
import java.time.Instant

/** Base class for an action a schedule can take. See ScheduleActionStartWorkflow for the most commonly used
  * implementation.
  */
sealed trait ZScheduleAction {
  def toJava: ScheduleAction
}

object ZScheduleAction {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  def fromJava(value: ScheduleAction): ZScheduleAction = {
    value match {
      case start: ScheduleActionStartWorkflow =>
        new StartWorkflow(start)
      case _ =>
        logger.warn(
          s"Unknown implementation of io.temporal.client.schedules.ScheduleAction found: class=${value.getClass} value=$value"
        )
        Unknown(value)
    }
  }

  /** Schedule action to start a workflow. */
  final class StartWorkflow @internalApi() (
    override val toJava: ScheduleActionStartWorkflow)
      extends ZScheduleAction {

    /** Get the workflow type name.
      *
      * @return
      *   the workflow type name.
      */
    def workflowType: String = toJava.getWorkflowType

    /** Get the workflow options used.
      * @return
      *   workflow options used for the scheduled workflows.
      */
    def options: WorkflowOptions = toJava.getOptions

    /** Get the headers that will be sent with each workflow scheduled.
      *
      * @return
      *   headers to be sent
      */
    def header: Header = toJava.getHeader

    /** Arguments for the workflow.
      *
      * @return
      *   the arguments used for the scheduled workflows.
      */
    def arguments: EncodedValues = toJava.getArguments

    override def toString: String = s"ZScheduleAction.StartWorkflow(" +
      s"workflowType=$workflowType" +
      s", options=$options" +
      s", header=$header" +
      s", arguments=$arguments" +
      s")"
  }

  /** Special case for subclasses unknown by zio-temporal
    */
  final case class Unknown private[zio] (
    toJava: ScheduleAction)
      extends ZScheduleAction
}

/** Information about when an action took place. */
final class ZScheduleActionResult private[zio] (val toJava: ScheduleActionResult) {

  /** Get the scheduled time of the action including jitter.
    *
    * @return
    *   scheduled time of action
    */
  def scheduledAt: Instant =
    toJava.getScheduledAt

  /** Get when the action actually started.
    *
    * @return
    *   time action actually started
    */
  def startedAt: Instant =
    toJava.getStartedAt

  /** Action that took place.
    *
    * @return
    *   action started
    */
  def action: ZScheduleActionExecution =
    ZScheduleActionExecution(toJava.getAction)

  override def toString: String = "ZScheduleActionResult(" +
    s"scheduledAt=$scheduledAt" +
    s", startedAt=$startedAt" +
    s", action=$action" +
    ")"
}

/** Base class for an action execution.
  */
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

  /** Action execution representing a scheduled workflow start. */

  final case class StartWorkflow private[zio] (toJava: ScheduleActionExecutionStartWorkflow)
      extends ZScheduleActionExecution {

    /** Get the workflow ID of the scheduled workflow.
      *
      * @return
      *   workflow ID
      */
    def workflowId: String = toJava.getWorkflowId

    /** Get the workflow run ID of the scheduled workflow.
      *
      * @return
      *   run ID
      */
    def firstExecutionRunId: String = toJava.getFirstExecutionRunId

    override def toString: String =
      s"ZScheduleActionExecution.StartWorkflow(" +
        s"workflowId=${workflowId}" +
        s", firstExecutionRunId=${firstExecutionRunId}" +
        s")"
  }

  /** Special case for subclasses unknown by zio-temporal
    */
  final case class Unknown private[zio] (toJava: ScheduleActionExecution) extends ZScheduleActionExecution
}
