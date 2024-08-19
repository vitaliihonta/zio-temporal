package zio.temporal.workflow

import zio.temporal.internal.ClassTagUtils
import scala.reflect.ClassTag

/** Type-safe wrapper of [[Class]]. The wrapper can be constructed only if the wrapped type [[T]] is a correct workflow
  * implementation
  * @tparam T
  *   workflow implementation type.
  *
  * @param runtimeClass
  *   the runtime class of the workflow
  */
final case class ZWorkflowImplementationClass[T] private[zio] (runtimeClass: Class[T]) {
  override def toString: String = {
    s"ZWorkflowImplementationClass(" +
      s"runtimeClass=$runtimeClass" +
      s")"
  }
}

object ZWorkflowImplementationClass {

  /** Constructs the wrapper.
    *
    * @tparam T
    *   workflow implementation type.
    * @return
    *   [[ZWorkflowImplementationClass]]
    */
  def apply[T: ClassTag: ExtendsWorkflow: IsConcreteClass: HasPublicNullaryConstructor]
    : ZWorkflowImplementationClass[T] =
    new ZWorkflowImplementationClass[T](ClassTagUtils.classOf[T])
}
