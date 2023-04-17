package zio.temporal.signal

import zio.temporal.internal.ZSignalMacro
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowStubSignalSyntax {
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
    *   Workflow execution
    */
  def signalWithStart(start: Unit, signal: Unit): TemporalIO[ZWorkflowExecution] =
    macro ZSignalMacro.signalWithStartImpl
}
