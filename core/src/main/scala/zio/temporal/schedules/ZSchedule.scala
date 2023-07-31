package zio.temporal.schedules

import io.temporal.client.schedules.{Schedule, ScheduleSpec, SchedulePolicy, ScheduleState}

// todo: implement
// TODO 2: think about the DSL for schedule starts
final case class ZSchedule(
  action: ZScheduleAction,
  spec:   ZScheduleSpec,
  policy: ZSchedulePolicy,
  state:  ZScheduleState) {

  def toJava: Schedule = ???
}

// todo: implement
final case class ZScheduleSpec() {
  def toJava: ScheduleSpec = ???
}

final case class ZSchedulePolicy() {
  def toJava: SchedulePolicy = ???
}

final case class ZScheduleState() {
  def toJava: ScheduleState = ???
}
