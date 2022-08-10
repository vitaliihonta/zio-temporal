package zio.temporal

import io.temporal.common.converter.Values
import io.temporal.failure.CanceledFailure

final class ZCanceledFailure private[zio] (val toJava: CanceledFailure) {
  def message: String = toJava.getMessage
  def details: Values = toJava.getDetails
}
