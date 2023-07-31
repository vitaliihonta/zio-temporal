package zio.temporal.schedules

import io.temporal.api.common.v1.Payload
import io.temporal.client.schedules.ScheduleDescription
import io.temporal.client.schedules.ScheduleListDescription
import zio.temporal.JavaTypeTag

import scala.jdk.CollectionConverters._

// todo: document
final class ZScheduleDescription private[zio] (val toJava: ScheduleDescription) {
  def id: String = toJava.getId

  def info: ZScheduleInfo =
    new ZScheduleInfo(toJava.getInfo)

  // todo: implement
  def schedule: ZSchedule =
    ZSchedule(
      action = ???,
      spec = ???,
      policy = ???,
      state = ???
    )

  def searchAttributes: Map[String, List[Any]] =
    toJava.getSearchAttributes.asScala.view.mapValues(_.asScala.toList).toMap

  def getMemo[T: JavaTypeTag](key: String): Option[T] =
    Option(toJava.getMemo[T](key, JavaTypeTag[T].klass, JavaTypeTag[T].genericType))
}

// todo: implement
final class ZScheduleListDescription private[zio] (val toJava: ScheduleListDescription) {
  def scheduleId: String = toJava.getScheduleId

}
