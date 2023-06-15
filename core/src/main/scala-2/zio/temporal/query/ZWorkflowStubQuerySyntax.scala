package zio.temporal.query

import zio.temporal.TemporalIO
import zio.temporal.internal.ZWorkflowQueryMacro

import scala.language.experimental.macros

trait ZWorkflowStubQuerySyntax {

  /** Queries the given workflow. Accepts the workflow query method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *   val result: TemporalIO[R] = ZWorkflowStub.query(
    *     stub.someQuery()
    *   )
    * }}}
    *
    * @tparam R
    *   query method result type
    * @param f
    *   the query method invocation
    * @return
    *   the query method result
    */
  def query[R](f: R): TemporalIO[R] =
    macro ZWorkflowQueryMacro.newQueryImpl[R]
}
