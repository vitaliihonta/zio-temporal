package zio.temporal.internal

import scala.reflect.ClassTag

private[zio] trait Stubs[A] {

  /** Stub is a compile-time view of temporal's runtime abstractions, such as Workflows.
    *
    * @note
    *   Do not call methods of proxied workflows directly! It should be used only within special methods like
    *   `ZWorkflowStub.signal`, `ZWorkflowStub.start`, `ZWorkflowStub.execute` and so on.
    * @tparam T
    *   compile-time view
    */
  final type Of[+T] = A & T

  private[zio] def Of[T: ClassTag](value: A)(implicit A: ClassTag[A]): Of[T] =
    StubProxies.proxy[A, T](value)

  type Ops[T]
}
