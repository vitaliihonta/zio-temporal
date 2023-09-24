package zio.temporal.activity

import zio._

/** Type-safe wrapper of activity implementation object. The wrapper can be constructed only if the wrapped object is a
  * correct activity implementation.
  *
  * @param value
  *   the activity implementation object
  */
final class ZActivityImplementationObject[T <: AnyRef] private[zio] (val value: T) {
  override def toString: String = {
    s"ZActivityImplementation(value=$value)"
  }

  override def hashCode(): Int = value.hashCode()

  override def equals(obj: Any): Boolean = {
    if (obj == null) false
    else
      obj match {
        case that: ZActivityImplementationObject[_] =>
          this.value == that.value
        case _ => false
      }
  }
}

object ZActivityImplementationObject {

  /** Constructs the wrapper.
    *
    * @tparam T
    *   activity type.
    *
    * @param value
    *   the correct activity implementation object
    * @return
    *   [[ZActivityImplementationObject]]
    */
  def apply[T <: AnyRef: ExtendsActivity](value: T): ZActivityImplementationObject[T] =
    new ZActivityImplementationObject[T](value)

  /** Constructs the wrapper from an ZIO environment */
  def service[T <: AnyRef: ExtendsActivity: Tag]: URIO[T, ZActivityImplementationObject[T]] =
    ZIO.serviceWith[T](ZActivityImplementationObject[T](_))

  /** Constructs the wrapper from a ZLayer */
  def layer[R, E, T <: AnyRef: ExtendsActivity: Tag](
    value: ZLayer[R, E, T]
  ): ZLayer[R, E, ZActivityImplementationObject[T]] =
    value >>> ZLayer.fromZIO(service[T])
}
