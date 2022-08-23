package zio.temporal.workflow

import zio.temporal.internal.MacroUtils
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

trait IsConcreteClassImplicits {
  implicit def materialize[A]: IsConcreteClass[A] =
    macro IsConcreteClassImplicits.IsConcreteClassMacro.materializeImpl[A]
}

private[zio] object IsConcreteClassImplicits {
  class IsConcreteClassMacro(override val c: blackbox.Context) extends MacroUtils(c) {
    import c.universe._

    def materializeImpl[A: WeakTypeTag]: Expr[IsConcreteClass[A]] = {
      assertConcreteClass(weakTypeOf[A].dealias)

      reify {
        IsConcreteClass.__zio_temporal_IsConcreateClassInstance.asInstanceOf[IsConcreteClass[A]]
      }
    }
  }
}
