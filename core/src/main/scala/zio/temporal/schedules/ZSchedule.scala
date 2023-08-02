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

  def withSpec(value: ZScheduleSpec): ZSchedule =
    copy(spec = value)

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

  final class WithAction private[zio] (private val action: ZScheduleAction) extends AnyVal {
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
  times:        ZScheduleSpec.Times,
  startAt:      Option[Instant],
  endAt:        Option[Instant],
  skip:         List[ZScheduleCalendarSpec],
  jitter:       Option[Duration],
  timeZoneName: Option[String]) {

  def withStartAt(value: Instant): ZScheduleSpec =
    copy(startAt = Some(value))

  def withEndAt(value: Instant): ZScheduleSpec =
    copy(endAt = Some(value))

  def withJitter(value: Duration): ZScheduleSpec =
    copy(jitter = Some(value))

  def withTimeZoneName(value: String): ZScheduleSpec =
    copy(timeZoneName = Some(value))

  def withSkip(values: List[ZScheduleCalendarSpec]): ZScheduleSpec =
    copy(skip = values)

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
  def calendars(values: List[ZScheduleCalendarSpec]): ZScheduleSpec =
    initial(Times.Calendars(values))

  def calendars(values: ZScheduleCalendarSpec*): ZScheduleSpec =
    calendars(values.toList)

  def intervals(values: List[ZScheduleIntervalSpec]): ZScheduleSpec =
    initial(Times.Intervals(values))

  def intervals(values: ZScheduleIntervalSpec*): ZScheduleSpec =
    intervals(values.toList)

  def cronExpressions(values: List[String]): ZScheduleSpec =
    initial(Times.CronExpressions(values))

  def cronExpressions(values: String*): ZScheduleSpec =
    cronExpressions(values.toList)

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

  def fromJava(spec: ScheduleSpec): ZScheduleSpec =
    ZScheduleSpec(
      times = Times.fromJava(spec),
      startAt = Option(spec.getStartAt),
      endAt = Option(spec.getEndAt),
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
