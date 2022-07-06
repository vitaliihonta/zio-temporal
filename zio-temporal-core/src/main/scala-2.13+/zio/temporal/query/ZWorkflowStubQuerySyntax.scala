package zio.temporal.query

import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.internal.ZWorkflowQueryMacro

import scala.language.experimental.macros

trait ZWorkflowStubQuerySyntax {
  def query[R](f: R): TemporalIO[TemporalClientError, R] =
    macro ZWorkflowQueryMacro.newQueryImpl[R]

  def query[E, R](f: Either[E, R]): TemporalIO[TemporalError[E], R] =
    macro ZWorkflowQueryMacro.newQueryEitherImpl[E, R]
}
