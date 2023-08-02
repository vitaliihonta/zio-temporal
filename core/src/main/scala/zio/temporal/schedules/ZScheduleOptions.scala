package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleOptions
import zio.temporal.ZSearchAttribute

import scala.jdk.CollectionConverters._

/** Options for creating a schedule. */
final case class ZScheduleOptions private[zio] (
  triggerImmediately: Boolean,
  backfills:          List[ZScheduleBackfill],
  memo:               Map[String, AnyRef],
  searchAttributes:   Map[String, ZSearchAttribute]) {

  /** Set if the schedule will be triggered immediately upon creation. */
  def withTriggerImmediately(value: Boolean): ZScheduleOptions =
    copy(triggerImmediately = value)

  /** Set the time periods to take actions on as if that time passed right now. */
  def withBackfills(values: ZScheduleBackfill*): ZScheduleOptions =
    copy(backfills = values.toList)

  /** Set the memo for the schedule. Values for the memo cannot be null. */
  def withMemo(values: (String, AnyRef)*): ZScheduleOptions =
    copy(memo = values.toMap)

  /** Set the search attributes for the schedule.
    * @note
    *   it uses untyped search attributes under the hood. todo: update once java SDK updates it
    */
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

  /** Default schedule options */
  val default: ZScheduleOptions =
    ZScheduleOptions(
      triggerImmediately = false,
      backfills = Nil,
      memo = Map.empty,
      searchAttributes = Map.empty
    )
}
