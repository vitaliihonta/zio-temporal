package ztemporal.internal

private[ztemporal] object tagging {
  sealed trait Tag[U] extends Any

  type @@[+T, U] = T with Tag[U]
  def tagged[T, U](value: T): T @@ U = value.asInstanceOf[T @@ U]

  private[ztemporal] trait Tagged {
    sealed trait Tagged

    /** Tagged type
      *
      * @tparam A
      *   raw type
      */
    type Of[+A] = A @@ Tagged

    private[ztemporal] def Of[A](value: A): Of[A] = tagged[A, Tagged](value)

    type Ops[A]
  }
}
