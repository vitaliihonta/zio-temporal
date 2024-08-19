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
  state: ZScheduleState) {

  /** Update the action for this schedule. Required to build.
    *
    * @see
    *   [[ZScheduleAction]]
    */
  def withAction(action: ZScheduleAction): ZSchedule =
    copy(action = action)

  /** Set the spec for this schedule. Required to build.
    *
    * @see
    *   [[ZScheduleSpec]]
    */
  def withSpec(value: ZScheduleSpec): ZSchedule =
    copy(spec = value)

  /** Set the policy for this schedule
    *
    * @see
    *   [[ZSchedulePolicy]]
    */
  def withPolicy(value: ZSchedulePolicy): ZSchedule =
    copy(policy = value)

  /** Set the state for this schedule
    *
    * @see
    *   [[ZScheduleState]]
    */
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

  /** Set the action for this schedule. Required to build.
    *
    * @see
    *   [[ZScheduleAction]]
    */
  def withAction(action: ZScheduleAction) =
    new WithAction(action)

  final class WithAction private[zio] (private val action: ZScheduleAction) extends AnyVal {

    /** Set the spec for this schedule. Required to build.
      *
      * @see
      *   [[ZScheduleSpec]]
      */
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
  * The [[times]] is a union represented as [[ZScheduleSpec.Times]] type. [[ZScheduleSpec.skip]] is used for exclusions
  */
final case class ZScheduleSpec private[zio] (
  times:   ZScheduleSpec.Times,
  startAt: Option[Instant],
  endAt:   Option[Instant],
  skip:    List[ZScheduleCalendarSpec],
  jitter:  Option[Duration],
  timeZoneName: Option[String]) {

  /** Set the start time of the schedule, before which any matching times will be skipped. */
  def withStartAt(value: Instant): ZScheduleSpec =
    copy(startAt = Some(value))

  /** Set the end time of the schedule, after which any matching times will be skipped. */
  def withEndAt(value: Instant): ZScheduleSpec =
    copy(endAt = Some(value))

  /** Set the jitter to apply to each action.
    *
    * <p>An action's schedule time will be incremented by a random value between 0 and this value if present (but not
    * past the next schedule).
    */
  def withJitter(value: Duration): ZScheduleSpec =
    copy(jitter = Some(value))

  /** Set the schedules time zone as a string, for example <c>US/Central</c>. */
  def withTimeZoneName(value: String): ZScheduleSpec =
    copy(timeZoneName = Some(value))

  /** Set the calendar-based specification of times a schedule should not run. */
  def withSkip(values: List[ZScheduleCalendarSpec]): ZScheduleSpec =
    copy(skip = values)

  /** Set the calendar-based specification of times a schedule should not run. */
  def withSkip(values: ZScheduleCalendarSpec*): ZScheduleSpec =
    withSkip(values.toList)

  def toJava: ScheduleSpec = {
    val builder = ScheduleSpec
      .newBuilder()
      .setSkip(skip.map(_.toJava).asJava)

    times.applyTo(builder)
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
      s", times=$times" +
      s", skip=$skip" +
      s", jitter=$jitter" +
      s", timeZoneName=$timeZoneName" +
      s")"
  }
}

object ZScheduleSpec {

  /** Set the calendar-based specification of times a schedule should run. */
  def calendars(values: List[ZScheduleCalendarSpec]): ZScheduleSpec =
    initial(Times.Calendars(values))

  /** Set the calendar-based specification of times a schedule should run. */
  def calendars(values: ZScheduleCalendarSpec*): ZScheduleSpec =
    calendars(values.toList)

  /** Set the interval-based specification of times a schedule should run. */
  def intervals(values: List[ZScheduleIntervalSpec]): ZScheduleSpec =
    initial(Times.Intervals(values))

  /** Set the interval-based specification of times a schedule should run. */
  def intervals(values: ZScheduleIntervalSpec*): ZScheduleSpec =
    intervals(values.toList)

  /** Set the cron-based specification of times a schedule should run.
    *
    * <p>This is provided for easy migration from legacy string-based cron scheduling. New uses should use [[calendars]]
    * instead. These expressions will be translated to calendar-based specifications on the server.
    */
  def cronExpressions(values: List[String]): ZScheduleSpec =
    initial(Times.CronExpressions(values))

  /** Set the cron-based specification of times a schedule should run.
    *
    * <p>This is provided for easy migration from legacy string-based cron scheduling. New uses should use [[calendars]]
    * instead. These expressions will be translated to calendar-based specifications on the server.
    */
  def cronExpressions(values: String*): ZScheduleSpec =
    cronExpressions(values.toList)

  def fromJava(spec: ScheduleSpec): ZScheduleSpec =
    ZScheduleSpec(
      times = Times.fromJava(spec),
      startAt = Option(spec.getStartAt),
      endAt = Option(spec.getEndAt),
      skip = Option(spec.getSkip).toList.flatMap(_.asScala.map(ZScheduleCalendarSpec.fromJava)),
      jitter = Option(spec.getJitter),
      timeZoneName = Option(spec.getTimeZoneName)
    )

  /** The [[Times]] is a schedule specification kind represented as a union of
    *   - [[Times.Intervals]]
    *   - [[Times.Calendars]]
    *   - [[Times.CronExpressions]]
    */
  sealed trait Times extends Product with Serializable {
    protected[temporal] def applyTo(builder: ScheduleSpec.Builder): Unit
  }

  final object Times {
    def fromJava(spec: ScheduleSpec): Times = {
      def asCalendars = Option(spec.getCalendars)
        .filterNot(_.isEmpty)
        .map(_.asScala.map(ZScheduleCalendarSpec.fromJava).toList)
        .map(Calendars(_))

      def asIntervals = Option(spec.getIntervals)
        .filterNot(_.isEmpty)
        .map(_.asScala.map(ZScheduleIntervalSpec.fromJava).toList)
        .map(Intervals(_))

      def asCronExpressions = Option(spec.getCronExpressions)
        .filterNot(_.isEmpty)
        .map(_.asScala.toList)
        .map(CronExpressions(_))

      asCalendars
        .orElse(asIntervals)
        .orElse(asCronExpressions)
        .getOrElse(Calendars(values = Nil))
    }

    final case class Calendars(values: List[ZScheduleCalendarSpec]) extends Times {
      override protected[temporal] def applyTo(builder: ScheduleSpec.Builder): Unit = {
        builder.setCalendars(values.map(_.toJava).asJava)
      }

      override def toString: String = {
        s"ZScheduleSpec.Times.Calendars(values=${values.mkString("[", ", ", "]")})"
      }
    }

    final case class Intervals(values: List[ZScheduleIntervalSpec]) extends Times {
      override protected[temporal] def applyTo(builder: ScheduleSpec.Builder): Unit = {
        builder.setIntervals(values.map(_.toJava).asJava)
      }

      override def toString: String = {
        s"ZScheduleSpec.Times.Intervals(values=${values.mkString("[", ", ", "]")})"
      }
    }

    final case class CronExpressions(values: List[String]) extends Times {
      override protected[temporal] def applyTo(builder: ScheduleSpec.Builder): Unit = {
        builder.setCronExpressions(values.asJava)
      }

      override def toString: String = {
        s"ZScheduleSpec.Times.CronExpressions(values=${values.mkString("[", ", ", "]")})"
      }
    }
  }

  private def initial(times: ZScheduleSpec.Times): ZScheduleSpec = {
    new ZScheduleSpec(
      times,
      startAt = None,
      endAt = None,
      skip = Nil,
      jitter = None,
      timeZoneName = None
    )
  }
}

/** Policies of a schedule. */
final case class ZSchedulePolicy private[zio] (
  overlap:       Option[ScheduleOverlapPolicy],
  catchupWindow: Option[Duration],
  pauseOnFailure: Option[Boolean]) {

  /** Set the policy for what happens when an action is started while another is still running. */
  def withOverlap(value: ScheduleOverlapPolicy): ZSchedulePolicy =
    copy(overlap = Some(value))

  /** Set the amount of time in the past to execute missed actions after a Temporal server is unavailable.
    */
  def withCatchupWindow(value: Duration): ZSchedulePolicy =
    copy(catchupWindow = Some(value))

  /** Set whether to pause the schedule if an action fails or times out. */
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

  /** Default schedule policy
    */
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

/** State of a schedule. */
final case class ZScheduleState(
  note:          Option[String],
  paused:        Option[Boolean],
  limitedAction: Option[Boolean],
  remainingActions: Option[Long]) {

  /** Set a human-readable message for the schedule. */
  def withNote(value: String): ZScheduleState =
    copy(note = Some(value))

  /** Set whether this schedule is paused. */
  def withPaused(value: Boolean): ZScheduleState =
    copy(paused = Some(value))

  /** Set ,if true, whether remaining actions will be decremented for each action */
  def withLimitedAction(value: Boolean): ZScheduleState =
    copy(limitedAction = Some(value))

  /** Set the actions remaining on this schedule. Once this number hits 0, no further actions are scheduled
    * automatically.
    */
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

  /** Default schedule state */
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
