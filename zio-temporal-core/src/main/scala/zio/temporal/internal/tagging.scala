package zio.temporal.internal

private[zio] object tagging {
  sealed trait Tag[U] extends Any

  type @@[+T, U] = T with Tag[U]
  def tagged[T, U](value: T): T @@ U = value.asInstanceOf[T @@ U]

  private[zio] trait Tagged {
    sealed trait Tagged

    /** Tagged type
      *
      * @tparam A
      *   raw type
      */
    type Of[+A] = A @@ Tagged

    private[zio] def Of[A](value: A): Of[A] = tagged[A, Tagged](value)

    type Ops[A]
  }

  private[zio] trait Proxies[A] extends Tagged {
    type Proxy[+T] <: T with A
    private[zio] def Proxy[T](value: A): Proxy[T] = value.asInstanceOf[Proxy[T]]
  }
}
