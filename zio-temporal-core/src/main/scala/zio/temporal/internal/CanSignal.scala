package zio.temporal.internal

import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.signal.ZInput
import zio.temporal.signal.ZSignal

protected[zio] trait CanSignal[Self] extends Any {
  protected[zio] def self: Self

  protected[zio] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit

  /** Signals temporal workflow.
    *
    * @param zsignal
    *   signal method to be invoked
    * @return
    *   signal result
    */
  def signal(zsignal: ZSignal[Any, ZSignal.SignalMethod]): TemporalIO[TemporalClientError, Unit] =
    signal[Any](zsignal)(())

  /** Signals temporal workflow.
    *
    * @tparam A
    *   signal input type
    * @param zsignal
    *   signal method to be invoked
    * @param input
    *   signal input
    * @return
    *   signal result
    */
  def signal[A](
    zsignal:            ZSignal[A, ZSignal.SignalMethod]
  )(input:              A
  )(implicit inputFrom: ZInput.From[A]
  ): TemporalIO[TemporalClientError, Unit] =
    TemporalInteraction.from {
      val signalName = zsignal.tpe.signalName
      val args       = inputFrom(input).args.asInstanceOf[Seq[AnyRef]]
      signalMethod(signalName, args)
    }
}
