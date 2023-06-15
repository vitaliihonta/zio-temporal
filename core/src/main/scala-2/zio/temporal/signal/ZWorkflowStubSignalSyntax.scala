package zio.temporal.signal

import zio.temporal.internal.ZSignalMacro
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowStubSignalSyntax {

  /** Sends a signal to the workflow. Accepts the signal method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *  val signalSent: TemporalIO[Unit] = ZWorkflowStub.signal(
    *     stub.someSignalMethod(someArg)
    *   )
    * }}}
    *
    * @param f
    *   the signal method invocation
    * @return
    *   ZIO
    */
  def signal(f: Unit): TemporalIO[Unit] =
    macro ZSignalMacro.signalImpl
}

trait ZWorkflowClientSignalWithStartSyntax {

  /** Performs signal with start atomically.
    *
    * @param start
    *   workflow method call
    * @param signal
    *   signal method call
    * @return
    *   Workflow execution metadata
    */
  def signalWithStart(start: Unit, signal: Unit): TemporalIO[ZWorkflowExecution] =
    macro ZSignalMacro.signalWithStartImpl
}
