package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleListDescription

// todo: implement
final class ZScheduleListDescription private[zio] (val toJava: ScheduleListDescription) {
  def scheduleId: String = toJava.getScheduleId

}
