package zio.temporal.schedules

import io.temporal.api.enums.v1.ScheduleOverlapPolicy
import io.temporal.client.schedules.{
  Schedule,
  ScheduleCalendarSpec,
  ScheduleIntervalSpec,
  SchedulePolicy,
  ScheduleSpec,
  ScheduleState
}
import zio._
import java.time.Instant

// todo: add withXXX and defaults
final case class ZSchedule(
  action: ZScheduleAction,
  spec:   ZScheduleSpec,
  policy: ZSchedulePolicy,
  state:  ZScheduleState) {

  def toJava: Schedule = {
    Schedule
      .newBuilder()
      .setAction(action.toJava)
      .setSpec(spec.toJava)
      .setPolicy(policy.toJava)
      .setState(state.toJava)
      .build()
  }
}

// todo: add withXXX and defaults
final case class ZScheduleSpec(
  calendars:       List[ScheduleCalendarSpec],
  intervals:       List[ScheduleIntervalSpec],
  cronExpressions: List[String],
  skip:            List[ScheduleCalendarSpec],
  startAt:         Instant,
  endAt:           Instant,
  jitter:          Duration,
  timeZoneName:    String) {

  def toJava: ScheduleSpec = ???
}

// todo: add withXXX methods and defaults
final case class ZSchedulePolicy(
  overlap:        ScheduleOverlapPolicy,
  catchupWindow:  Duration,
  pauseOnFailure: Boolean) {

  def toJava: SchedulePolicy = {
    SchedulePolicy
      .newBuilder()
      .setOverlap(overlap)
      .setCatchupWindow(catchupWindow)
      .setPauseOnFailure(pauseOnFailure)
      .build()
  }
}

// todo: add withXXX methods and defaults
final case class ZScheduleState(
  note:             String,
  paused:           Boolean,
  limitedAction:    Boolean,
  remainingActions: Long) {

  def toJava: ScheduleState = {
    ScheduleState
      .newBuilder()
      .setNote(note)
      .setPaused(paused)
      .setLimitedAction(limitedAction)
      .setRemainingActions(remainingActions)
      .build()
  }
}
