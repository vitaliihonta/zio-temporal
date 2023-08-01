package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleDescription
import io.temporal.client.schedules.ScheduleListDescription
import zio.temporal.JavaTypeTag

import scala.jdk.CollectionConverters._

// todo: document
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

  // todo: add toString
}

// todo: implement
final class ZScheduleListDescription private[zio] (val toJava: ScheduleListDescription) {
  def scheduleId: String = toJava.getScheduleId

}
