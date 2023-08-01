package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleOptions
import scala.jdk.CollectionConverters._

final case class ZScheduleOptions private[zio] (
  triggerImmediately: Boolean,
  backfills:          List[ZScheduleBackfill],
  memo:               Map[String, AnyRef]
  // todo: add search attributes
) {

  def withTriggerImmediately(value: Boolean): ZScheduleOptions =
    copy(triggerImmediately = value)

  def withBackfills(values: ZScheduleBackfill*): ZScheduleOptions =
    copy(backfills = values.toList)

  def withMemo(values: (String, AnyRef)*): ZScheduleOptions =
    copy(memo = values.toMap)

  def toJava: ScheduleOptions = {
    ScheduleOptions
      .newBuilder()
      .setTriggerImmediately(triggerImmediately)
      .setBackfills(backfills.view.map(_.toJava).toList.asJava)
      .setMemo(memo.asJava)
      .build()
  }
}

object ZScheduleOptions {
  val default: ZScheduleOptions =
    ZScheduleOptions(
      triggerImmediately = false,
      backfills = Nil,
      memo = Map.empty
    )
}
