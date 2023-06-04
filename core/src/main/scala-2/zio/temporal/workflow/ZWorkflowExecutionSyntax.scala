package zio.temporal.workflow

import zio._
import zio.temporal.internal.ZWorkflowMacro
import zio.temporal._
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowExecutionSyntax {
  def start[A](f: A): TemporalIO[ZWorkflowExecution] =
    macro ZWorkflowMacro.startImpl[A]

  def execute[R](f: R): TemporalIO[R] =
    macro ZWorkflowMacro.executeImpl[R]

  def executeWithTimeout[R](timeout: Duration)(f: R): TemporalIO[R] =
    macro ZWorkflowMacro.executeWithTimeoutImpl[R]
}
