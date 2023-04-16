package zio.temporal.signal

import zio.temporal.internal.ZChildSignalMacro
import scala.language.experimental.macros
trait ZChildWorkflowStubSignalSyntax {
  def signal(f: Unit): Unit =
    macro ZChildSignalMacro.signalImpl
}
