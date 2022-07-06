package zio.temporal.signal

import zio.temporal.internal.ZSignalMacro
import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.internalApi
import zio.temporal.workflow.ZWorkflowStub

import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowStubSignalSyntax {
  def signal(f: Unit): TemporalIO[TemporalClientError, Unit] =
    macro ZSignalMacro.signalImpl
}
