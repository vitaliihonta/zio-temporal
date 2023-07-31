package zio.temporal.schedules

import io.temporal.api.enums.v1.ScheduleOverlapPolicy
import io.temporal.client.schedules.ScheduleHandle
import zio.temporal.TemporalIO
import zio.temporal.internal.TemporalInteraction

import scala.jdk.CollectionConverters._

// todo: document
/** Handle for interacting with a schedule.
  * @see
  *   [[ScheduleHandle]]
  */
final class ZScheduleHandle private[zio] (val toJava: ScheduleHandle) {
  def id: String = toJava.getId

  def describe: TemporalIO[ZScheduleDescription] =
    TemporalInteraction.from {
      new ZScheduleDescription(
        toJava.describe()
      )
    }

  def backfill(backfills: List[ZScheduleBackfill]): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.backfill(backfills.view.map(_.toJava).toList.asJava)
    }

  def delete(): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.delete()
    }

  def pause(note: Option[String] = None): TemporalIO[Unit] =
    TemporalInteraction.from {
      note.fold(ifEmpty = toJava.pause())(toJava.pause)
    }

  def trigger(overlapPolicy: Option[ScheduleOverlapPolicy] = None): TemporalIO[Unit] =
    TemporalInteraction.from {
      overlapPolicy.fold(ifEmpty = toJava.trigger())(toJava.trigger)
    }

  def unpause(note: Option[String] = None): TemporalIO[Unit] =
    TemporalInteraction.from {
      note.fold(ifEmpty = toJava.unpause())(toJava.unpause)
    }

  // todo: add update
}
