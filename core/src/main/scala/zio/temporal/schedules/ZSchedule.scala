package zio.temporal.schedules

import io.temporal.api.enums.v1.ScheduleOverlapPolicy
import io.temporal.client.schedules._
import zio.{Schedule => _, _}
import java.time.Instant
import scala.jdk.CollectionConverters._

/** A schedule for periodically running an action. */
final case class ZSchedule private[zio] (
  action: ZScheduleAction,
  spec:   ZScheduleSpec,
  policy: ZSchedulePolicy,
  state:  ZScheduleState) {

  def withPolicy(value: ZSchedulePolicy): ZSchedule =
    copy(policy = value)

  def withState(value: ZScheduleState): ZSchedule =
    copy(state = value)

  def toJava: Schedule = Schedule
    .newBuilder()
    .setAction(action.toJava)
    .setSpec(spec.toJava)
    .setPolicy(policy.toJava)
    .setState(state.toJava)
    .build()

  override def toString: String = {
    s"ZSchedule(" +
      s"action=$action" +
      s", spec=$spec" +
      s", policy=$policy" +
      s", state=$state" +
      s")"
  }
}

object ZSchedule {
  def withAction(action: ZScheduleAction): WithAction =
    new WithAction(action)

  final class WithAction private[zio] (val action: ZScheduleAction) extends AnyVal {
    def withSpec(spec: ZScheduleSpec): ZSchedule =
      ZSchedule(
        action,
        spec,
        policy = ZSchedulePolicy.default,
        state = ZScheduleState.default
      )
  }

  def fromJava(schedule: Schedule): ZSchedule =
    ZSchedule(
      action = ZScheduleAction.fromJava(schedule.getAction),
      spec = ZScheduleSpec.fromJava(schedule.getSpec),
      policy = ZSchedulePolicy.fromJava(schedule.getPolicy),
      state = ZScheduleState.fromJava(schedule.getState)
    )
}

/** Specification of the times scheduled actions may occur.
  *
  * The times are the union of [[ZScheduleSpec.calendars]], [[ZScheduleSpec.intervals]], and
  * [[ZScheduleSpec.cronExpressions]] excluding anything in [[ZScheduleSpec.skip]].
  */
final case class ZScheduleSpec private[zio] (
  startAt:         Option[Instant],
  endAt:           Option[Instant],
  calendars:       List[ZScheduleCalendarSpec],
  intervals:       List[ZScheduleIntervalSpec],
  cronExpressions: List[String],
  skip:            List[ZScheduleCalendarSpec],
  jitter:          Option[Duration],
  timeZoneName:    Option[String]) {

  def withStartAt(value: Instant): ZScheduleSpec =
    copy(startAt = Some(value))

  def withEndAt(value: Instant): ZScheduleSpec =
    copy(endAt = Some(value))

  def withCalendars(values: ZScheduleCalendarSpec*): ZScheduleSpec =
    copy(calendars = values.toList)

  def withIntervals(values: ZScheduleIntervalSpec*): ZScheduleSpec =
    copy(intervals = values.toList)

  def withCronExpressions(values: String*): ZScheduleSpec =
    copy(cronExpressions = values.toList)

  def withJitter(value: Duration): ZScheduleSpec =
    copy(jitter = Some(value))

  def withTimeZoneName(value: String): ZScheduleSpec =
    copy(timeZoneName = Some(value))

  def toJava: ScheduleSpec = {
    val builder = ScheduleSpec
      .newBuilder()
      .setCalendars(calendars.map(_.toJava).asJava)
      .setIntervals(intervals.map(_.toJava).asJava)
      .setCronExpressions(cronExpressions.asJava)
      .setSkip(skip.map(_.toJava).asJava)

    startAt.foreach(builder.setStartAt)
    endAt.foreach(builder.setEndAt)
    jitter.foreach(builder.setJitter)
    timeZoneName.foreach(builder.setTimeZoneName)

    builder.build()
  }

  override def toString: String = {
    s"ZScheduleSpec(" +
      s"startAt=$startAt" +
      s", endAt=$endAt" +
      s", calendars=$calendars" +
      s", intervals=$intervals" +
      s", cronExpressions=$cronExpressions" +
      s", skip=$skip" +
      s", jitter=$jitter" +
      s", timeZoneName=$timeZoneName" +
      s")"
  }
}

object ZScheduleSpec {
  def apply(
    startAt:         Option[Instant] = None,
    endAt:           Option[Instant] = None,
    calendars:       List[ZScheduleCalendarSpec] = Nil,
    intervals:       List[ZScheduleIntervalSpec] = Nil,
    cronExpressions: List[String] = Nil,
    skip:            List[ZScheduleCalendarSpec] = Nil,
    jitter:          Option[Duration] = None,
    timeZoneName:    Option[String] = None
  ): ZScheduleSpec = {
    new ZScheduleSpec(
      startAt,
      endAt,
      calendars,
      intervals,
      cronExpressions,
      skip,
      jitter,
      timeZoneName
    )
  }

  def fromJava(spec: ScheduleSpec): ZScheduleSpec =
    ZScheduleSpec(
      startAt = Option(spec.getStartAt),
      endAt = Option(spec.getEndAt),
      calendars = Option(spec.getCalendars).toList.flatMap(_.asScala.map(ZScheduleCalendarSpec.fromJava)),
      intervals = Option(spec.getIntervals).toList.flatMap(_.asScala.map(ZScheduleIntervalSpec.fromJava)),
      cronExpressions = Option(spec.getCronExpressions).toList.flatMap(_.asScala),
      skip = Option(spec.getSkip).toList.flatMap(_.asScala.map(ZScheduleCalendarSpec.fromJava)),
      jitter = Option(spec.getJitter),
      timeZoneName = Option(spec.getTimeZoneName)
    )
}

/** Policies of a schedule. */
final case class ZSchedulePolicy private[zio] (
  overlap:        Option[ScheduleOverlapPolicy],
  catchupWindow:  Option[Duration],
  pauseOnFailure: Option[Boolean]) {

  def withOverlap(value: ScheduleOverlapPolicy): ZSchedulePolicy =
    copy(overlap = Some(value))

  def withCatchupWindow(value: Duration): ZSchedulePolicy =
    copy(catchupWindow = Some(value))

  def withPauseOnFailure(value: Boolean): ZSchedulePolicy =
    copy(pauseOnFailure = Some(value))

  def toJava: SchedulePolicy = {
    val builder = SchedulePolicy.newBuilder()
    overlap.foreach(builder.setOverlap)
    catchupWindow.foreach(builder.setCatchupWindow)
    pauseOnFailure.foreach(builder.setPauseOnFailure)
    builder.build()
  }

  override def toString: String = {
    s"ZSchedulePolicy(" +
      s"overlap=$overlap" +
      s", catchupWindow=$catchupWindow" +
      s", pauseOnFailure=$pauseOnFailure" +
      s")"
  }
}

object ZSchedulePolicy {
  val default: ZSchedulePolicy =
    ZSchedulePolicy(
      overlap = None,
      catchupWindow = None,
      pauseOnFailure = None
    )

  def fromJava(policy: SchedulePolicy): ZSchedulePolicy =
    ZSchedulePolicy(
      overlap = Option(policy.getOverlap),
      catchupWindow = Option(policy.getCatchupWindow),
      pauseOnFailure = Some(policy.isPauseOnFailure)
    )
}

final case class ZScheduleState(
  note:             Option[String],
  paused:           Option[Boolean],
  limitedAction:    Option[Boolean],
  remainingActions: Option[Long]) {

  def withNote(value: String): ZScheduleState =
    copy(note = Some(value))

  def withPaused(value: Boolean): ZScheduleState =
    copy(paused = Some(value))

  def withLimitedAction(value: Boolean): ZScheduleState =
    copy(limitedAction = Some(value))

  def withRemainingActions(value: Long): ZScheduleState =
    copy(remainingActions = Some(value))

  def toJava: ScheduleState = {
    val builder = ScheduleState.newBuilder()

    note.foreach(builder.setNote)
    paused.foreach(builder.setPaused)
    limitedAction.foreach(builder.setLimitedAction)
    remainingActions.foreach(builder.setRemainingActions)
    builder.build()
  }

  override def toString: String = {
    s"ZScheduleState(" +
      s"note=$note" +
      s", paused=$paused" +
      s", limitedAction=$limitedAction" +
      s", remainingActions=$remainingActions" +
      s")"
  }
}

object ZScheduleState {
  val default: ZScheduleState =
    ZScheduleState(
      note = None,
      paused = None,
      limitedAction = None,
      remainingActions = None
    )

  def fromJava(state: ScheduleState): ZScheduleState =
    ZScheduleState(
      note = Option(state.getNote),
      paused = Some(state.isPaused),
      limitedAction = Some(state.isLimitedAction),
      remainingActions = Some(state.getRemainingActions)
    )
}
