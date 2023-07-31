package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleHandle

// todo: implement
final class ZScheduleHandle private[zio] (val toJava: ScheduleHandle) {}
