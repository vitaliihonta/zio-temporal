package zio.temporal.failure

object ActivityFailure {
  object Cause {
    def unapply(error: io.temporal.failure.ActivityFailure): Option[Throwable] =
      Option(error.getCause)
  }
}
