package zio.temporal.internal

import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.signal.ZSignal

protected[zio] trait CanSignal[Self] extends Any {
  protected[zio] def self: Self

  protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit

  def signal(zsignal: ZSignal.Signal): TemporalIO[TemporalClientError, Unit] =
    TemporalInteraction.from {
      signalMethod(zsignal.name, zsignal.args)
    }
}
