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
final class WorkflowImplementationClass[T] private (val runtimeClass: Class[T]) {}

object WorkflowImplementationClass {

  /** Constructs the wrapper.
    *
    * @tparam T
    *   workflow implementation type.
    * @return
    *   [[WorkflowImplementationClass]]
    */
  def apply[
    T: ClassTag: ExtendsWorkflow: IsConcreteClass: HasPublicNullaryConstructor
  ]: WorkflowImplementationClass[T] =
    new WorkflowImplementationClass[T](ClassTagUtils.classOf[T])
}
