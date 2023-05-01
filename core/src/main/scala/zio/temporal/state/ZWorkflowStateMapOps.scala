package zio.temporal.state

import scala.language.implicitConversions

trait ZWorkflowStateMapSyntax {

  /** Initializes the state with an empty map
    *
    * @tparam K
    *   map key type
    * @tparam V
    *   map value type
    * @return
    *   the state
    */
  def emptyMap[K, V]: ZWorkflowState.Required[Map[K, V]] =
    ZWorkflowState.make(Map.empty[K, V])

  implicit def ZWorkflowStateMapOps[K, V](self: ZWorkflowState[Map[K, V]]): ZWorkflowStateMapOps[K, V] =
    new ZWorkflowStateMapOps[K, V](self)
}

final class ZWorkflowStateMapOps[K, V](val self: ZWorkflowState[Map[K, V]]) {
  def get(key: K): Option[V] = self.toOption.flatMap(_.get(key))

  def getOrElse(key: K, default: => V): V =
    get(key).getOrElse(default)

  def apply(key: K): V = self.snapshot.apply(key)

  def filterKeysInPlace(p: K => Boolean): self.type =
    filterInPlace((k, _) => p(k))

  def filterInPlace(p: (K, V) => Boolean): self.type =
    self.update(_.view.filter(p.tupled).toMap)

  def update(key: K, value: V): self.type =
    self.update(_.updated(key, value))

  def +=(pair: (K, V)): self.type =
    self.update(_.updated(pair._1, pair._2))

  def ++=(values: Iterable[(K, V)]): self.type =
    self.update(_ ++ values)

  def clear(): self.type =
    self := Map.empty

  def remove(key: K): self.type =
    self.update(_ - key)

  def -=(key: K): self.type =
    self.update(_ - key)

  def --=(keys: Iterable[K]): self.type =
    self.update(_ -- keys)
}
