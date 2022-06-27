package zio.temporal

import java.time.{Instant, LocalDateTime, ZoneOffset}

/** Represents current timestamp in epoch millis format
  */
class ZCurrentTimeMillis private[zio] (val toEpochMillis: Long) extends AnyVal {

  @inline def toLocalDateTime(offset: ZoneOffset = ZoneOffset.UTC): LocalDateTime =
    toInstant
      .atOffset(offset)
      .toLocalDateTime

  @inline def toInstant: Instant =
    Instant.ofEpochMilli(toEpochMillis)
}
