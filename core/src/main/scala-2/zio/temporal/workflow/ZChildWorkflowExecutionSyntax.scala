package zio.temporal.workflow

import zio.temporal.internal.ZChildWorkflowMacro
import scala.language.experimental.macros

trait ZChildWorkflowExecutionSyntax {
  def execute[R](f: R): R =
    macro ZChildWorkflowMacro.executeImpl[R]

  def executeAsync[R](f: R): ZAsync[R] =
    macro ZChildWorkflowMacro.executeAsyncImpl[R]
}

// TODO: implement
trait ZExternalWorkflowExecutionSyntax {}
