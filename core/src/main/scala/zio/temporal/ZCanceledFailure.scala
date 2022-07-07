package zio.temporal

import io.temporal.common.converter.Values
import io.temporal.failure.CanceledFailure

class ZCanceledFailure private[zio] (val toJava: CanceledFailure) extends AnyVal {
  def message: String = toJava.getMessage
  def details: Values = toJava.getDetails
}
