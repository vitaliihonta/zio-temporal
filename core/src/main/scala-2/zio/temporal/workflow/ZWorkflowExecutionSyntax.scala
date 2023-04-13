package zio.temporal.workflow

import zio.temporal.internal.ZWorkflowMacro
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowExecutionSyntax {
  def start[A](f: A): TemporalIO[ZWorkflowExecution] =
    macro ZWorkflowMacro.startImpl[A]

  def execute[R](f: R): TemporalIO[R] =
    macro ZWorkflowMacro.executeImpl[R]

  def async[R](f: R): ZAsync[R] =
    macro ZWorkflowMacro.asyncImpl[R]
}
