package ztemporal

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Represents current timestamp in epoch millis format
  */
class ZCurrentTimeMillis private[ztemporal] (val toEpochMillis: Long) extends AnyVal {

  @inline def toLocalDateTime(offset: ZoneOffset = ZoneOffset.UTC): LocalDateTime =
    toInstant
      .atOffset(offset)
      .toLocalDateTime

  @inline def toInstant: Instant =
    Instant.ofEpochMilli(toEpochMillis)
}
