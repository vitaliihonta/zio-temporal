package zio.temporal.schedules

import io.temporal.client.schedules.{ScheduleCalendarSpec, ScheduleIntervalSpec, ScheduleRange}
import zio._
import scala.language.implicitConversions
import scala.jdk.CollectionConverters._

// todo: document
final case class ZScheduleIntervalSpec private[zio] (
  every:  Duration,
  offset: Option[Duration]) {

  def withOffset(value: Duration): ZScheduleIntervalSpec =
    copy(offset = Some(value))

  def toJava: ScheduleIntervalSpec =
    new ScheduleIntervalSpec(every, offset.orNull)

  override def toString: String = {
    s"ZScheduleIntervalSpec(" +
      s"every=$every" +
      s", offset=$offset" +
      s")"
  }
}

object ZScheduleIntervalSpec {
  def fromJava(value: ScheduleIntervalSpec): ZScheduleIntervalSpec =
    ZScheduleIntervalSpec(
      every = value.getEvery,
      offset = Option(value.getOffset)
    )
}

// todo: document
final case class ZScheduleRange private[zio] (start: Int, end: Int, step: Int) {

  def toJava: ScheduleRange = new ScheduleRange(start, end, step)

  override def toString: String = {
    s"ZScheduleRange(" +
      s"start=$start" +
      s", end=$end" +
      s", step=$step" +
      s")"
  }
}

object ZScheduleRange {
  def fromJava(value: ScheduleRange): ZScheduleRange =
    ZScheduleRange(
      start = value.getStart,
      end = value.getEnd,
      step = value.getStep
    )
}

// todo: document
final case class ZScheduleCalendarSpec private[zio] (
  seconds:    List[ZScheduleRange],
  minutes:    List[ZScheduleRange],
  hour:       List[ZScheduleRange],
  dayOfMonth: List[ZScheduleRange],
  month:      List[ZScheduleRange],
  year:       List[ZScheduleRange],
  dayOfWeek:  List[ZScheduleRange],
  comment:    Option[String]) {

  def withSeconds(values: ZScheduleRange*): ZScheduleCalendarSpec =
    withSeconds(values.toList)

  def withSeconds(values: List[ZScheduleRange]): ZScheduleCalendarSpec =
    copy(seconds = values)

  def withMinutes(values: ZScheduleRange*): ZScheduleCalendarSpec =
    withMinutes(values.toList)

  def withMinutes(values: List[ZScheduleRange]): ZScheduleCalendarSpec =
    copy(minutes = values)

  def withHour(values: ZScheduleRange*): ZScheduleCalendarSpec =
    withHour(values.toList)

  def withHour(values: List[ZScheduleRange]): ZScheduleCalendarSpec =
    copy(hour = values)

  def withDayOfMonth(values: ZScheduleRange*): ZScheduleCalendarSpec =
    withDayOfMonth(values.toList)

  def withDayOfMonth(values: List[ZScheduleRange]): ZScheduleCalendarSpec =
    copy(dayOfMonth = values)

  def withMonth(values: ZScheduleRange*): ZScheduleCalendarSpec =
    withMonth(values.toList)

  def withMonth(values: List[ZScheduleRange]): ZScheduleCalendarSpec =
    copy(month = values)

  def withYear(values: ZScheduleRange*): ZScheduleCalendarSpec =
    withYear(values.toList)

  def withYear(values: List[ZScheduleRange]): ZScheduleCalendarSpec =
    copy(year = values)

  def withDayOfWeek(values: ZScheduleRange*): ZScheduleCalendarSpec =
    withDayOfWeek(values.toList)

  def withDayOfWeek(values: List[ZScheduleRange]): ZScheduleCalendarSpec =
    copy(dayOfWeek = values)

  def withComment(value: String): ZScheduleCalendarSpec =
    copy(comment = Some(value))

  def toJava: ScheduleCalendarSpec = {
    val builder = ScheduleCalendarSpec
      .newBuilder()
      .setSeconds(seconds.map(_.toJava).asJava)
      .setMinutes(minutes.map(_.toJava).asJava)
      .setHour(hour.map(_.toJava).asJava)
      .setDayOfMonth(dayOfMonth.map(_.toJava).asJava)
      .setMonth(month.map(_.toJava).asJava)
      .setYear(year.map(_.toJava).asJava)
      .setDayOfWeek(dayOfWeek.map(_.toJava).asJava)

    comment.foreach(builder.setComment)

    builder.build()
  }

  override def toString: String = {
    s"ZScheduleCalendarSpec(" +
      s"seconds=$seconds" +
      s", minutes=$minutes" +
      s", hour=$hour" +
      s", dayOfMonth=$dayOfMonth" +
      s", month=$month" +
      s", year=$year" +
      s", dayOfWeek=$dayOfWeek" +
      s", comment=$comment" +
      s")"
  }
}

object ZScheduleCalendarSpec {
  def fromJava(value: ScheduleCalendarSpec): ZScheduleCalendarSpec = {
    def listFromJava(values: java.util.List[ScheduleRange]): List[ZScheduleRange] =
      values.asScala.map(ZScheduleRange.fromJava).toList

    ZScheduleCalendarSpec(
      seconds = listFromJava(value.getSeconds),
      minutes = listFromJava(value.getMinutes),
      hour = listFromJava(value.getHour),
      dayOfMonth = listFromJava(value.getHour),
      month = listFromJava(value.getMonth),
      year = listFromJava(value.getYear),
      dayOfWeek = listFromJava(value.getDayOfWeek),
      comment = Option(value.getComment)
    )
  }
}

// todo: document
trait ScheduleSpecSyntax {
  final def every(value: Duration): ZScheduleIntervalSpec =
    ZScheduleIntervalSpec(value, offset = None)

  final def range(from: Int = 0, to: Int = 0, by: Int = 0): ZScheduleRange =
    ZScheduleRange(start = from, end = to, step = by)

  final val calendar: ZScheduleCalendarSpec =
    ZScheduleCalendarSpec(
      seconds = Nil,
      minutes = Nil,
      hour = Nil,
      dayOfMonth = Nil,
      month = Nil,
      year = Nil,
      dayOfWeek = Nil,
      comment = None
    )

  // todo: add other useful constants?
  final val allMonthDays: List[ZScheduleRange] =
    List(range(from = 1, to = 31, by = 1))

  final val allMonths: List[ZScheduleRange] =
    List(range(from = 1, to = 12, by = 1))

  final val allWeekDays: List[ZScheduleRange] =
    List(range(from = 0, to = 6, by = 1))
}
