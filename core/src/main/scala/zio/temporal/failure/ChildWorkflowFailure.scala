package zio.temporal.failure

object ChildWorkflowFailure {
  object Cause {
    def unapply(failure: io.temporal.failure.ChildWorkflowFailure): Option[Throwable] =
      Option(failure.getCause)
  }
}
