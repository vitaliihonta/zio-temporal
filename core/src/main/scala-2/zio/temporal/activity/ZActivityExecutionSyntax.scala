package zio.temporal.activity

import zio.temporal.internal.ZActivityStubMacro
import zio.temporal.workflow.ZAsync
import scala.language.experimental.macros

trait ZActivityExecutionSyntax {

  /** Executes the given activity synchronously. Accepts the activity method invocation
    *
    * Example:
    * {{{
    *   val stub: ZActivityStub.Of[T] = ???
    *
    *   val result: R = ZActivityStub.execute(
    *     stub.someMethod(someArg)
    *   )
    * }}}
    *
    * @tparam R
    *   activity result type
    * @param f
    *   the activity invocation
    * @return
    *   the activity result
    */
  def execute[R](f: R): R =
    macro ZActivityStubMacro.executeImpl[R]

  /** Executes the given activity asynchronously. Accepts the activity method invocation
    *
    * Example:
    * {{{
    *   val stub: ZActivityStub.Of[T] = ???
    *
    *   val result: ZAsync[R] = ZActivityStub.executeAsync(
    *     stub.someMethod(someArg)
    *   )
    * }}}
    *
    * @tparam R
    *   activity result type
    * @param f
    *   the activity execution invocation
    * @return
    *   the activity result (async)
    */
  def executeAsync[R](f: R): ZAsync[R] =
    macro ZActivityStubMacro.executeAsyncImpl[R]
}
