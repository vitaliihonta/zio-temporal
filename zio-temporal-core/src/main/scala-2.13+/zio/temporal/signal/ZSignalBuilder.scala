package zio.temporal.signal

import zio.temporal.internal.ZSignalMacro
import zio.temporal.internalApi

import scala.language.experimental.macros

class ZSignalBuilder private[zio] () {
  def start[A](f: A): ZSignalWithStartBuilder =
    macro ZSignalMacro.startImpl[A]

  def signal(f: Unit): ZSignal.Signal =
    macro ZSignalMacro.signalImpl
}

class ZSignalWithStartBuilder @internalApi() (val __zio_temporal_Signal: ZSignal.Signal) {
  def signal(f: Unit): ZSignal.WithStart = ???
}
