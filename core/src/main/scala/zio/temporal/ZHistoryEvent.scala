package zio.temporal

import io.temporal.api.history.v1.{HistoryEvent, History}
import java.time.Instant
import io.temporal.api.enums.v1.EventType

/** NOTE: the wrapper is incomplete, contains only basic information
  */
final class ZHistoryEvent(val toJava: HistoryEvent) {
  toJava.getWorkflowExecutionFailedEventAttributes
  def eventId: Long =
    toJava.getEventId

  def eventTime: Instant = {
    val time = toJava.getEventTime
    Instant.ofEpochSecond(time.getSeconds, time.getNanos)
  }

  def eventType: EventType =
    toJava.getEventType

  override def toString: String =
    s"ZHistoryEvent(eventId=$eventId, eventTime=$eventTime, " +
      s"eventType=$eventType" +
      s")"
}
