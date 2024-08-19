package zio.temporal.schedules

import io.temporal.api.enums.v1.ScheduleOverlapPolicy
import io.temporal.client.schedules.ScheduleBackfill

import java.time.Instant

/** Time period and policy for actions taken as if their scheduled time has already passed.
  * @param startAt
  *   Start of the range to evaluate the schedule in. This is exclusive.
  * @param endAt
  *   End of the range to evaluate the schedule in. This is inclusive.
  * @param overlapPolicy
  *   Overlap policy to use for this backfill request.
  */
final case class ZScheduleBackfill(
  startAt: Instant,
  endAt:   Instant,
  overlapPolicy: ScheduleOverlapPolicy = ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_UNSPECIFIED) {

  /** Set the start of the range to evaluate the schedule in. This is exclusive.
    */
  def withStartAt(value: Instant): ZScheduleBackfill =
    copy(startAt = value)

  /** Set the end of the range to evaluate the schedule in. This is inclusive.
    */
  def withEndAt(value: Instant): ZScheduleBackfill =
    copy(endAt = value)

  /** Set the overlap policy to use for this backfill request.
    */
  def withOverlapPolicy(value: ScheduleOverlapPolicy): ZScheduleBackfill =
    copy(overlapPolicy = value)

  /** Converts schedule backfill to Java SDK's [[ScheduleBackfill]]
    */
  def toJava: ScheduleBackfill =
    new ScheduleBackfill(startAt, endAt, overlapPolicy)

  override def toString: String = {
    s"ZScheduleBackfill(" +
      s"startAt=$startAt" +
      s", endAt=$endAt" +
      s", overlapPolicy=$overlapPolicy" +
      s")"
  }
}

object ZScheduleBackfill {
  def fromJava(backfill: ScheduleBackfill): ZScheduleBackfill =
    ZScheduleBackfill(
      startAt = backfill.getStartAt,
      endAt = backfill.getEndAt,
      overlapPolicy = backfill.getOverlapPolicy
    )
}
