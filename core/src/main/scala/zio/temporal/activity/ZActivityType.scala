package zio.temporal.activity

import scala.reflect.ClassTag
import zio.temporal.simpleNameOf

/** Type-safe wrapper of raw activity type. The wrapper can be constructed only if the wrapped type is a correct
  * activity implementation. Horewer, there is an option to construct it with arbitrary activity name in for untyped
  * stubs.
  *
  * @param activityType
  *   the type of the activity
  */
final case class ZActivityType private[zio] (activityType: String) {
  override def toString: String = {
    s"ZActivityType(" +
      s"activityType=$activityType" +
      s")"
  }
}

object ZActivityType {

  /** Constructs the wrapper.
    *
    * @tparam T
    *   activity type.
    * @return
    *   [[ZActivityType]]
    */
  def apply[T: ClassTag: IsActivity]: ZActivityType = {
    new ZActivityType(simpleNameOf[T])
  }

  /** Constructs the wrapper. Use it only when working with untyped activities.
    *
    * @param value
    *   activity type.
    * @return
    *   [[ZActivityType]]
    */
  def untyped(value: String): ZActivityType = {
    new ZActivityType(value)
  }
}
