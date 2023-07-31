package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleOptions
import scala.jdk.CollectionConverters._

final case class ZScheduleOptions(
  triggerImmediately: Boolean,
  backfills:          List[ZScheduleBackfill] = Nil,
  memo:               Map[String, AnyRef] = Map()
  // todo: add search attributes
) {

  def toJava: ScheduleOptions = {
    ScheduleOptions
      .newBuilder()
      .setTriggerImmediately(triggerImmediately)
      .setBackfills(backfills.view.map(_.toJava).toList.asJava)
      .setMemo(memo.asJava)
      .build()
  }
}
