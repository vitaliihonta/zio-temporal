package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleOptions
import zio.temporal.ZSearchAttribute

import scala.jdk.CollectionConverters._

final case class ZScheduleOptions private[zio] (
  triggerImmediately: Boolean,
  backfills:          List[ZScheduleBackfill],
  memo:               Map[String, AnyRef],
  searchAttributes:   Map[String, ZSearchAttribute]) {

  def withTriggerImmediately(value: Boolean): ZScheduleOptions =
    copy(triggerImmediately = value)

  def withBackfills(values: ZScheduleBackfill*): ZScheduleOptions =
    copy(backfills = values.toList)

  def withMemo(values: (String, AnyRef)*): ZScheduleOptions =
    copy(memo = values.toMap)

  def withSearchAttributes(attrs: Map[String, ZSearchAttribute]): ZScheduleOptions =
    copy(searchAttributes = attrs)

  def toJava: ScheduleOptions = {
    ScheduleOptions
      .newBuilder()
      .setTriggerImmediately(triggerImmediately)
      .setBackfills(backfills.view.map(_.toJava).toList.asJava)
      .setMemo(memo.asJava)
      .setSearchAttributes(searchAttributes)
      .build()
  }

  override def toString: String = {
    s"ZScheduleOptions(" +
      s"triggerImmediately=$triggerImmediately" +
      s", backfills=$backfills" +
      s", memo=$memo" +
      s", searchAttributes=$searchAttributes" +
      s")"
  }
}

object ZScheduleOptions {
  val default: ZScheduleOptions =
    ZScheduleOptions(
      triggerImmediately = false,
      backfills = Nil,
      memo = Map.empty,
      searchAttributes = Map.empty
    )
}
