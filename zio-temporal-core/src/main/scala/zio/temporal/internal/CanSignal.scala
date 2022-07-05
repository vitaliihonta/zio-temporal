package zio.temporal.internal

import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.internalApi
import zio.temporal.signal.ZSignal

protected[zio] trait BaseCanSignal extends Any

protected[zio] trait CanSignal[Self] extends Any with BaseCanSignal {
  protected[zio] def self: Self

  protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit

  @internalApi
  def __zio_temporal_invokeSignal(zsignal: ZSignal.Signal): TemporalIO[TemporalClientError, Unit] =
    TemporalInteraction.from {
      signalMethod(zsignal.name, zsignal.args)
    }
}
