package zio.temporal.workflow

import zio._
import zio.temporal.internal.ZWorkflowMacro
import zio.temporal._
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowExecutionSyntax {

  /** Starts the given workflow. '''Doesn't wait''' for the workflow to finish. Accepts the workflow method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *   val workflowExecution: TemporalIO[ZWorkflowExecution] =
    *      ZWorkflowStub.start(
    *        stub.someMethod(someArg)
    *      )
    * }}}
    *
    * @tparam A
    *   workflow result type
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow execution metadata
    */
  def start[A](f: A): TemporalIO[ZWorkflowExecution] =
    macro ZWorkflowMacro.startImpl[A]

  /** Executes the given workflow. '''Waits''' for the workflow to finish. Accepts the workflow method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *   val workflowExecution: TemporalIO[R] =
    *      ZWorkflowStub.execute(
    *        stub.someMethod(someArg)
    *      )
    * }}}
    *
    * @tparam R
    *   workflow result type
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow result
    */
  def execute[R](f: R): TemporalIO[R] =
    macro ZWorkflowMacro.executeImpl[R]

  /** Executes the given workflow with a given timeout. '''Waits''' for the workflow to finish. Accepts the workflow
    * method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *   val workflowExecution: TemporalIO[R] =
    *      ZWorkflowStub.executeWithTimeout(5.seconds)(
    *        stub.someMethod(someArg)
    *      )
    * }}}
    *
    * @tparam R
    *   workflow result type
    * @param timeout
    *   the timeout
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow result
    */
  def executeWithTimeout[R](timeout: Duration)(f: R): TemporalIO[R] =
    macro ZWorkflowMacro.executeWithTimeoutImpl[R]
}
