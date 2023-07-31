package zio.temporal.schedules

import io.temporal.api.enums.v1.ScheduleOverlapPolicy
import io.temporal.client.schedules.ScheduleBackfill

import java.time.Instant

/** Time period and policy for actions taken as if their scheduled time has already passed. */
final case class ZScheduleBackfill(
  startAt:       Instant,
  endAt:         Instant,
  overlapPolicy: ScheduleOverlapPolicy = ScheduleOverlapPolicy.SCHEDULE_OVERLAP_POLICY_UNSPECIFIED) {

  def toJava =
    new ScheduleBackfill(startAt, endAt, overlapPolicy)
}
