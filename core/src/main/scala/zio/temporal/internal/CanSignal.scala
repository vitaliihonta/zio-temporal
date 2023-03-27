package zio.temporal.internal

import zio.temporal.TemporalIO
import zio.temporal.internalApi

@internalApi
trait BaseCanSignal

@internalApi
trait CanSignal[Self] extends BaseCanSignal {
  def toJava: Self

  protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit

  @internalApi
  def __zio_temporal_invokeSignal(signalName: String, args: Seq[AnyRef]): TemporalIO[Unit] =
    TemporalInteraction.from {
      signalMethod(signalName, args)
    }
}
