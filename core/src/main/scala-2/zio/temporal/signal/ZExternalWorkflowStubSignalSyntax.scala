package zio.temporal.signal

import zio.temporal.internal.ZExternalSignalMacro
import scala.language.experimental.macros

trait ZExternalWorkflowStubSignalSyntax {

  /** Sends a signal to an external workflow. Accepts the signal method invocation
    *
    * Example:
    * {{{
    *   val stub: ZExternalWorkflowStub.Of[T] = ???
    *
    *   ZExternalWorkflowStub.signal(
    *     stub.someSignalMethod(someArg)
    *   )
    * }}}
    *
    * @param f
    *   the signal method invocation
    */
  def signal(f: Unit): Unit =
    macro ZExternalSignalMacro.signalImpl
}
