package zio.temporal.workflow

import zio.temporal.internal.ZChildWorkflowMacro
import scala.language.experimental.macros

trait ZChildWorkflowExecutionSyntax {

  /** Executes the given child workflow synchronously. Accepts the workflow method invocation
    *
    * Example:
    * {{{
    *   val stub: ZChildWorkflowStub.Of[T] = ???
    *
    *   val result: R = ZChildWorkflowStub.execute(
    *     stub.someMethod(someArg)
    *   )
    * }}}
    *
    * @tparam R
    *   workflow result type
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow result
    */
  def execute[R](f: R): R =
    macro ZChildWorkflowMacro.executeImpl[R]

  /** Executes the given child workflow asynchronously. Accepts the workflow method invocation
    *
    * Example:
    * {{{
    *   val stub: ZChildWorkflowStub.Of[T] = ???
    *
    *   val result: ZAsync[R] = ZChildWorkflowStub.executeAsync(
    *     stub.someMethod(someArg)
    *   )
    * }}}
    *
    * @tparam R
    *   workflow result type
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow result (async)
    */
  def executeAsync[R](f: R): ZAsync[R] =
    macro ZChildWorkflowMacro.executeAsyncImpl[R]
}
