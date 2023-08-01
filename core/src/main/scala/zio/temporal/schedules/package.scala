package zio.temporal

import io.temporal.client.schedules.ScheduleRange
import scala.language.implicitConversions

package object schedules {
  implicit def scheduleRangeFromScalaRange(range: Range.Inclusive): ScheduleRange =
    new ScheduleRange(range.start, range.end, range.step)
}
