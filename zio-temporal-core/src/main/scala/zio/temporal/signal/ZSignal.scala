package zio.temporal.signal

import io.temporal.client.BatchRequest

sealed trait ZSignal extends Serializable

object ZSignal {
  class Signal(private[zio] val name: String, private[zio] val args: Seq[AnyRef]) extends ZSignal

  class WithStart(private[zio] val addRequests: BatchRequest => Unit) extends ZSignal
}
