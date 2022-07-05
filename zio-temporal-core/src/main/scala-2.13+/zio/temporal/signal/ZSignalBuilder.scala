package zio.temporal.signal

import zio.temporal.internal.ZSignalMacro
import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.internalApi
import zio.temporal.workflow.ZWorkflowStub

import scala.language.experimental.macros
import scala.language.implicitConversions

class ZSignalBuilder private[zio] () {
//  def start[A](f: A): ZSignalWithStartBuilder =
//    macro ZSignalMacro.startImpl[A]

//  def signal(f: Unit): ZSignal.Signal =
//    macro ZSignalMacro.signalImpl
}

trait ZWorkflowStubProxySyntax {
  def signal(f: Unit): TemporalIO[TemporalClientError, Unit] =
    macro ZSignalMacro.signalImpl
}
