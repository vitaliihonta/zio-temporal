package zio.temporal.schedules

import io.temporal.api.enums.v1.ScheduleOverlapPolicy
import io.temporal.client.schedules.{ScheduleHandle, ScheduleUpdate, ScheduleUpdateInput}
import zio.temporal.TemporalIO
import zio.temporal.internal.TemporalInteraction
import scala.jdk.CollectionConverters._

/** Handle for interacting with a schedule.
  * @see
  *   [[ScheduleHandle]]
  */
final class ZScheduleHandle private[zio] (val toJava: ScheduleHandle) {

  /** Get this schedule's ID.
    *
    * @return
    *   the schedule's ID
    */
  def id: String = toJava.getId

  /** Fetch this schedule's description.
    *
    * @return
    *   description of the schedule
    */
  def describe: TemporalIO[ZScheduleDescription] =
    TemporalInteraction.from {
      new ZScheduleDescription(
        toJava.describe()
      )
    }

  /** Backfill this schedule by going through the specified time periods as if they passed right now.
    *
    * @param backfills
    *   backfill requests to run
    */
  def backfill(backfills: List[ZScheduleBackfill]): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.backfill(backfills.view.map(_.toJava).toList.asJava)
    }

  /** Delete this schedule. */
  def delete(): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.delete()
    }

  /** Pause this schedule.
    *
    * @param note
    *   to set the schedule state.
    */
  def pause(note: Option[String] = None): TemporalIO[Unit] =
    TemporalInteraction.from {
      note.fold(ifEmpty = toJava.pause())(toJava.pause)
    }

  /** Trigger an action on this schedule to happen immediately.
    *
    * @param overlapPolicy
    *   override the schedule overlap policy.
    */
  def trigger(overlapPolicy: Option[ScheduleOverlapPolicy] = None): TemporalIO[Unit] =
    TemporalInteraction.from {
      overlapPolicy.fold(ifEmpty = toJava.trigger())(toJava.trigger)
    }

  /** Unpause this schedule.
    *
    * @param note
    *   to set the schedule state.
    */
  def unpause(note: Option[String] = None): TemporalIO[Unit] =
    TemporalInteraction.from {
      note.fold(ifEmpty = toJava.unpause())(toJava.unpause)
    }

  /** Update this schedule. This is done via a callback which can be called multiple times in case of conflict.
    *
    * @param updater
    *   Callback to invoke with the current update input. The result can be null to signify no update to perform, or a
    *   schedule update instance with a schedule to perform an update.
    */
  def update(updater: ZScheduleUpdateInput => ZScheduleUpdate): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.update { updateInput =>
        updater(
          ZScheduleUpdateInput.fromJava(updateInput)
        ).toJava
      }
    }
}

/** Parameter passed to a schedule updater. */
final case class ZScheduleUpdateInput(
  description: ZScheduleDescription) {

  override def toString: String = {
    s"ZScheduleUpdateInput(" +
      s"description=$description" +
      s")"
  }
}

object ZScheduleUpdateInput {
  def fromJava(input: ScheduleUpdateInput): ZScheduleUpdateInput =
    ZScheduleUpdateInput(new ZScheduleDescription(input.getDescription))
}

/** An update returned from a schedule updater. */
final case class ZScheduleUpdate(
  schedule: ZSchedule) {

  def toJava =
    new ScheduleUpdate(schedule.toJava)

  override def toString: String = {
    s"ZScheduleUpdate(" +
      s"schedule=$schedule" +
      s")"
  }
}
