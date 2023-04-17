package zio.temporal.signal

import zio.temporal.internal.ZExternalSignalMacro
import scala.language.experimental.macros

trait ZExternalWorkflowStubSignalSyntax {
  def signal(f: Unit): Unit =
    macro ZExternalSignalMacro.signalImpl
}
