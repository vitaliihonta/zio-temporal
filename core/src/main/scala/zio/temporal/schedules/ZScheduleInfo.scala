package zio.temporal.schedules

import io.temporal.client.schedules.{ScheduleInfo, ScheduleListInfo}
import java.time.Instant
import scala.jdk.CollectionConverters._

/** Information about a schedule. */
final class ZScheduleInfo private[zio] (val toJava: ScheduleInfo) {

  /** Get the number of actions taken by the schedule.
    *
    * @return
    *   number of actions taken
    */
  def numActions: Long = toJava.getNumActions

  /** Get the number of actions skipped due to missing the catchup window.
    *
    * @return
    *   number of actions skipped due to catchup window
    */
  def numActionsMissedCatchupWindow: Long = toJava.getNumActionsMissedCatchupWindow

  /** Get the number of actions skipped due to overlap.
    *
    * @return
    *   number of actions skipped due to overlap
    */
  def numActionsSkippedOverlap: Long = toJava.getNumActionsSkippedOverlap

  /** Get a list of currently running actions.
    *
    * @return
    *   list of currently running actions
    */
  def runningActions: List[ZScheduleActionExecution] =
    toJava.getRunningActions.asScala.view.map(ZScheduleActionExecution(_)).toList

  /** Get a list of the most recent actions, oldest first.
    *
    * @return
    *   list of the most recent actions
    */
  def recentActions: List[ZScheduleActionResult] =
    toJava.getRecentActions.asScala.view.map(new ZScheduleActionResult(_)).toList

  /** Get a list of the next scheduled action times.
    *
    * @return
    *   list of the next recent times
    */
  def nextActionTimes: List[Instant] =
    toJava.getNextActionTimes.asScala.toList

  /** Get the time the schedule was created at.
    *
    * @return
    *   time the schedule was created
    */
  def createdAt: Instant =
    toJava.getCreatedAt

  /** Get the last time the schedule was updated.
    *
    * @return
    *   last time the schedule was updated
    */
  def lastUpdatedAt: Instant =
    toJava.getLastUpdatedAt

  override def toString: String = s"ZScheduleInfo(" +
    s"numActions=$numActions" +
    s", numActionsMissedCatchupWindow=$numActionsMissedCatchupWindow" +
    s", numActionsSkippedOverlap=$numActionsSkippedOverlap" +
    s", runningActions=${runningActions.mkString("[", ", ", "]")}" +
    s", recentActions=${recentActions.mkString("[", ", ", "]")}" +
    s", nextActionTimes=${nextActionTimes.mkString("[", ", ", "]")}" +
    s", createdAt=$createdAt" +
    s", lastUpdatedAt=$lastUpdatedAt" +
    s")"
}

/** Information about a listed schedule. */
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
