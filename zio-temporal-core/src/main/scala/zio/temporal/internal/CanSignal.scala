package zio.temporal.internal

import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.internalApi

protected[zio] trait BaseCanSignal extends Any

protected[zio] trait CanSignal[Self] extends Any with BaseCanSignal {
  def toJava: Self

  protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit

  @internalApi
  def __zio_temporal_invokeSignal(signalName: String, args: Seq[AnyRef]): TemporalIO[TemporalClientError, Unit] =
    TemporalInteraction.from {
      signalMethod(signalName, args)
    }
}
