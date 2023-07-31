package zio.temporal.schedules

import io.temporal.client.schedules.{ScheduleInfo, ScheduleListInfo}
import java.time.Instant
import scala.jdk.CollectionConverters._

// todo: document
final class ZScheduleInfo private[zio] (val toJava: ScheduleInfo) {
  def numActions: Long = toJava.getNumActions

  def numActionsMissedCatchupWindow: Long = toJava.getNumActionsMissedCatchupWindow

  def numActionsSkippedOverlap: Long = toJava.getNumActionsSkippedOverlap

  def runningActions: List[ZScheduleActionExecution] =
    toJava.getRunningActions.asScala.view.map(ZScheduleActionExecution(_)).toList

  def recentActions: List[ZScheduleActionResult] =
    toJava.getRecentActions.asScala.view.map(new ZScheduleActionResult(_)).toList

  def nextActionTimes: List[Instant] =
    toJava.getNextActionTimes.asScala.toList

  def createdAt: Instant =
    toJava.getCreatedAt

  def lastUpdatedAt: Instant =
    toJava.getLastUpdatedAt
}

final class ZScheduleListInfo private[zio] (val toJava: ScheduleListInfo) {

  /** Most recent actions, oldest first. This may be a smaller count than ScheduleInfo.RecentActions
    *
    * @return
    *   The most recent action
    */
  def recentActions: List[ZScheduleActionResult] =
    toJava.getRecentActions.asScala.view.map(new ZScheduleActionResult(_)).toList

  /** Next scheduled action times. This may be a smaller count than ScheduleInfo.NextActions. */
  def nextActionTimes: List[Instant] =
    toJava.getNextActionTimes.asScala.toList

  override def toString: String =
    s"ZScheduleListInfo(" +
      s"recentActions=${recentActions.mkString("[", ", ", "]")}" +
      s", nextActionTimes=${nextActionTimes.mkString("[", ", ", "]")}" +
      s")"
}
