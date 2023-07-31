package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleListInfo

// todo: implement
final class ZScheduleListInfo private[zio] (val toJava: ScheduleListInfo) {}
