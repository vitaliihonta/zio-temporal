package zio.temporal.workflow

import zio.temporal.internal.ZWorkflowMacro
import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowExecutionSyntax {
  def start[A](f: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    macro ZWorkflowMacro.startImpl[A]
}
