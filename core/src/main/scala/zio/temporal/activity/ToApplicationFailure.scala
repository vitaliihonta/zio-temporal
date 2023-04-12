package zio.temporal.activity

import zio.temporal.failure.ApplicationFailure

trait ToApplicationFailure[E] {
  def wrap(error: E): ApplicationFailure
}

object ToApplicationFailure extends LowPriorityToApplicationFailure {}

trait LowPriorityToApplicationFailure {

  /** By default, any typed error is considered retryable. The class name must be used in retry policies.
    */
  implicit def defaultToApplicationFailure[E]: ToApplicationFailure[E] =
    DefaultGenericToApplicationFailure.asInstanceOf[ToApplicationFailure[E]]
}

private[zio] object DefaultGenericToApplicationFailure extends ToApplicationFailure[AnyRef] {
  override def wrap(error: AnyRef): ApplicationFailure = {
    ApplicationFailure.newFailure(
      s"Activity failed with a typed error: $error",
      error.getClass.getName,
      error
    )
  }
}
