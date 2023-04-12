package zio.temporal.query

import zio.temporal.TemporalIO
import zio.temporal.internal.ZWorkflowQueryMacro

import scala.language.experimental.macros

trait ZWorkflowStubQuerySyntax {
  def query[R](f: R): TemporalIO[R] =
    macro ZWorkflowQueryMacro.newQueryImpl[R]
}
