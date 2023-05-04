package zio.temporal

package object failure {
  type ApplicationFailure   = io.temporal.failure.ApplicationFailure
  type ActivityFailure      = io.temporal.failure.ActivityFailure
  type ChildWorkflowFailure = io.temporal.failure.ChildWorkflowFailure

  implicit class ZioTemporalApplicationFailureSyntax(private val self: io.temporal.failure.ApplicationFailure)
      extends AnyVal {
    def getDetailsAs[E: TypeIsSpecified: JavaTypeTag]: E =
      self.getDetails.get(JavaTypeTag[E].klass, JavaTypeTag[E].genericType)
  }
}
