package ztemporal

import izumi.reflect.Tag

package object signal {

  /** Creates a single [[ZInput]]
    *
    * Requires implicit light type tag.
    *
    * @tparam A input type
    * @param value input value
    * @return [[ZInput]] with single value
    */
  def zinput[A: Tag](value: A): ZInput[A] = ZInput(value)
}
