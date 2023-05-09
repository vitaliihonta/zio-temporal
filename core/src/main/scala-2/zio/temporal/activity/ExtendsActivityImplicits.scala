package zio.temporal.activity

import zio.temporal.internal.InvocationMacroUtils
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

trait ExtendsActivityImplicits {
  implicit def materialize[A]: ExtendsActivity[A] =
    macro ExtendsActivityImplicits.ExtendsActivityMacro.materializeImpl[A]
}

private[zio] object ExtendsActivityImplicits {
  class ExtendsActivityMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
    import c.universe._

    def materializeImpl[A: WeakTypeTag]: Expr[ExtendsActivity[A]] = {
      assertExtendsActivity(weakTypeOf[A].dealias)

      reify {
        ExtendsActivity.__zio_temporal_ExtendsActivityInstance.asInstanceOf[ExtendsActivity[A]]
      }
    }
  }
}
