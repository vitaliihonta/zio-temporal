package ztemporal

import io.temporal.common.converter.Values
import io.temporal.failure.CanceledFailure

class ZCanceledFailure private[ztemporal] (val toJava: CanceledFailure) extends AnyVal {
  def message: String = toJava.getMessage
  def details: Values = toJava.getDetails
}
