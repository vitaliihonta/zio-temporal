package zio.temporal

import java.time.{Instant, LocalDateTime, OffsetDateTime, ZoneOffset}

/** Represents current timestamp in epoch millis format
  */
final class ZCurrentTimeMillis private[zio] (val toEpochMillis: Long) extends AnyVal {

  @inline def toLocalDateTime(offset: ZoneOffset = ZoneOffset.UTC): LocalDateTime =
    toOffsetDateTime(offset).toLocalDateTime

  @inline def toOffsetDateTime(offset: ZoneOffset = ZoneOffset.UTC): OffsetDateTime =
    toInstant.atOffset(offset)

  @inline def toInstant: Instant =
    Instant.ofEpochMilli(toEpochMillis)

  override def toString: String = {
    s"ZCurrentTimeMillis(" +
      s"epochMillis=$toEpochMillis" +
      s", pretty=$toInstant" +
      s")"
  }
}
