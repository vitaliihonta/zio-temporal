package zio.temporal.internal

import scala.reflect.ClassTag

private[zio] object tagging {
  sealed trait Tag[U] extends Any

  type @@[+T, U] = T & Tag[U]
  def tagged[T, U](value: T): T @@ U = value.asInstanceOf[T @@ U]

  private[zio] trait Proxies[A] {

    /** Proxy is a compile-time view of temporal's runtime abstractions, such as Workflows.
      * @note
      *   Do not call methods of proxied workflows directly! It should be used only within special methods like
      *   `ZWorkflowStub.signal`, `ZWorkflowStub.start`, `ZWorkflowStub.execute` and so on.
      *
      * @tparam T
      *   compile-time view
      */
    final type Proxy[+T] = A & T
    private[zio] def Proxy[T: ClassTag](value: A)(implicit A: ClassTag[A]): Proxy[T] =
      StubProxies.proxy[A, T](value)

    sealed trait Tagged

    /** Tagged type
      *
      * @tparam T
      *   raw type
      */
    type Of[+T] = T @@ Tagged

    private[zio] def Of[T](value: T): Of[T] = tagged[T, Tagged](value)

    type Ops[T]
  }
}
