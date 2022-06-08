package ztemporal.internal

import ztemporal.ZTemporalClientError
import ztemporal.ZTemporalIO
import ztemporal.signal.ZInput
import ztemporal.signal.ZSignal

protected[ztemporal] trait CanSignal[Self] extends Any {
  protected[ztemporal] def self: Self

  protected[ztemporal] def signalMethod(signalName: String, args: Seq[AnyRef]): Unit

  /** Signals temporal workflow.
    *
    * @param zsignal signal method to be invoked
    * @return signal result
    */
  def signal(zsignal: ZSignal[Any, ZSignal.SignalMethod]): ZTemporalIO[ZTemporalClientError, Unit] =
    signal[Any](zsignal)(())

  /** Signals temporal workflow.
    *
    * @tparam A signal input type
    * @param zsignal signal method to be invoked
    * @param input signal input
    * @return signal result
    */
  def signal[A](
    zsignal:            ZSignal[A, ZSignal.SignalMethod]
  )(input:              A
  )(implicit inputFrom: ZInput.From[A]
  ): ZTemporalIO[ZTemporalClientError, Unit] =
    TemporalInteraction.from {
      val signalName = zsignal.tpe.signalName
      val args       = inputFrom(input).args.asInstanceOf[Seq[AnyRef]]
      signalMethod(signalName, args)
    }
}
