package zio.temporal.signal

import zio.temporal.internal.ZChildSignalMacro
import scala.language.experimental.macros

trait ZChildWorkflowStubSignalSyntax {

  /** Sends a signal to the child workflow. Accepts the signal method invocation
    *
    * Example:
    * {{{
    *   val stub: ZChildWorkflowStub.Of[T] = ???
    *
    *   ZChildWorkflowStub.signal(
    *     stub.someSignalMethod(someArg)
    *   )
    * }}}
    *
    * @param f
    *   the signal method invocation
    */
  def signal(f: Unit): Unit =
    macro ZChildSignalMacro.signalImpl
}
