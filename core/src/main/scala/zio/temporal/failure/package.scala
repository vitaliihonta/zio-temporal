package zio.temporal

import zio.temporal.internal.ClassTagUtils

import scala.reflect.ClassTag

package object failure {
  type ApplicationFailure   = io.temporal.failure.ApplicationFailure
  type ActivityFailure      = io.temporal.failure.ActivityFailure
  type ChildWorkflowFailure = io.temporal.failure.ChildWorkflowFailure

  implicit class ZioTemporalApplicationFailureSyntax(private val self: io.temporal.failure.ApplicationFailure)
      extends AnyVal {
    def getDetailsAs[E: TypeIsSpecified: ClassTag]: E =
      self.getDetails.get(ClassTagUtils.classOf[E])
  }
}
