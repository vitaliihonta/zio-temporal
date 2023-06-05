package zio.temporal

package object failure {
  type ApplicationFailure   = io.temporal.failure.ApplicationFailure
  type ActivityFailure      = io.temporal.failure.ActivityFailure
  type ChildWorkflowFailure = io.temporal.failure.ChildWorkflowFailure
  type TimeoutFailure       = io.temporal.failure.TimeoutFailure
  type CanceledFailure      = io.temporal.failure.CanceledFailure

  implicit class ZioTemporalApplicationFailureSyntax(private val self: ApplicationFailure) extends AnyVal {
    def getDetailsAs[E: TypeIsSpecified: JavaTypeTag]: E =
      self.getDetails.get(JavaTypeTag[E].klass, JavaTypeTag[E].genericType)
  }

  implicit class ZioTemporalTimeoutFailureSyntax(private val self: TimeoutFailure) extends AnyVal {
    def getLastHeartbeatDetailsAs[E: TypeIsSpecified: JavaTypeTag]: E =
      self.getLastHeartbeatDetails.get(JavaTypeTag[E].klass, JavaTypeTag[E].genericType)
  }
}
