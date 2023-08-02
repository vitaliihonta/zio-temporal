package zio.temporal.schedules

import io.temporal.client.schedules.{
  ScheduleDescription,
  ScheduleListAction,
  ScheduleListActionStartWorkflow,
  ScheduleListDescription,
  ScheduleListSchedule,
  ScheduleListState
}
import org.slf4j.LoggerFactory
import zio.temporal.JavaTypeTag
import scala.jdk.CollectionConverters._

/** Description of a schedule. */
final class ZScheduleDescription private[zio] (val toJava: ScheduleDescription) {

  /** Get the ID of the schedule.
    *
    * @return
    *   schedule ID
    */
  def id: String = toJava.getId

  /** Get information about the schedule.
    *
    * @return
    *   schedule info
    */
  def info =
    new ZScheduleInfo(toJava.getInfo)

  /** Gets the schedule details.
    *
    * @return
    *   schedule details
    */
  def schedule: ZSchedule =
    ZSchedule.fromJava(toJava.getSchedule)

  /** Gets the search attributes on the schedule.
    *
    * @return
    *   search attributes
    */
  def searchAttributes: Map[String, List[Any]] =
    toJava.getSearchAttributes.asScala.view.map { case (k, v) => k -> v.asScala.toList }.toMap

  def getMemo[T: JavaTypeTag](key: String): Option[T] =
    Option(toJava.getMemo[T](key, JavaTypeTag[T].klass, JavaTypeTag[T].genericType))

  override def toString: String = {
    s"ZScheduleDescription(" +
      s"id=$id" +
      s", info=$info" +
      s", schedule=$schedule" +
      s", searchAttributes=$searchAttributes" +
      s")"
  }
}

/** Description of a listed schedule. */
final class ZScheduleListDescription private[zio] (val toJava: ScheduleListDescription) {
  def scheduleId: String =
    toJava.getScheduleId

  def schedule: ZScheduleListSchedule =
    new ZScheduleListSchedule(toJava.getSchedule)

  def info: ZScheduleListInfo =
    new ZScheduleListInfo(toJava.getInfo)

  def searchAttributes: Map[String, Any] =
    toJava.getSearchAttributes.asScala.toMap

  def getMemo[T: JavaTypeTag](key: String): Option[T] =
    Option(toJava.getMemo[T](key, JavaTypeTag[T].klass, JavaTypeTag[T].genericType))

  override def toString: String = {
    s"ZScheduleListDescription(" +
      s"scheduleId=$scheduleId" +
      s", schedule=$schedule" +
      s", info=$info" +
      s", searchAttributes=$searchAttributes" +
      s")"
  }
}

/** Details for a listed schedule. */
final class ZScheduleListSchedule private[zio] (val toJava: ScheduleListSchedule) {
  def action: ZScheduleListAction =
    ZScheduleListAction(toJava.getAction)

  def spec: ZScheduleSpec =
    ZScheduleSpec.fromJava(toJava.getSpec)

  def state: ZScheduleListState =
    new ZScheduleListState(toJava.getState)

  override def toString: String = {
    s"ZScheduleListSchedule(" +
      s"action=$action" +
      s", spec=$spec" +
      s", state=$state" +
      s")"
  }
}

/** State of a listed schedule. */
final class ZScheduleListState private[zio] (val toJava: ScheduleListState) {
  def note: Option[String] = Option(toJava.getNote)

  def paused: Boolean = toJava.isPaused

  override def toString: String = {
    s"ZScheduleListState(" +
      s"note=$note," +
      s"paused=$paused" +
      s")"
  }
}

/** Base class for an action a listed schedule can take. */
sealed trait ZScheduleListAction {
  def toJava: ScheduleListAction
}

object ZScheduleListAction {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  def apply(value: ScheduleListAction): ZScheduleListAction = value match {
    case value: ScheduleListActionStartWorkflow => new StartWorkflow(value)
    case _ =>
      logger.warn(
        s"Unknown implementation of io.temporal.client.schedules.ScheduleListAction found: class=${value.getClass} value=$value"
      )
      Unknown(value)
  }

  /** Action to start a workflow on a listed schedule. */
  final class StartWorkflow(val toJava: ScheduleListActionStartWorkflow) extends ZScheduleListAction {

    /** Get the scheduled workflow type name.
      *
      * @return
      *   Workflow type name
      */
    def workflow: String = toJava.getWorkflow

    override def toString: String = {
      s"ZScheduleListAction.StartWorkflow(" +
        s"workflow=$workflow" +
        s")"
    }
  }

  /** Special case for subclasses unknown by zio-temporal
    */
  final case class Unknown(toJava: ScheduleListAction) extends ZScheduleListAction {
    override def toString: String = {
      s"ZScheduleListAction.Unknown(" +
        s"toJava=$toJava" +
        s")"
    }
  }
}
