package zio.temporal.activity

import zio.temporal.internal.ZActivityStubMacro
import zio.temporal.workflow.ZAsync
import scala.language.experimental.macros

trait ZActivityExecutionSyntax {
  def execute[R](f: R): R =
    macro ZActivityStubMacro.executeImpl[R]

  def executeAsync[R](f: R): ZAsync[R] =
    macro ZActivityStubMacro.executeAsyncImpl[R]
}
