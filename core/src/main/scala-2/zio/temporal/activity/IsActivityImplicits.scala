package zio.temporal.activity

import zio.temporal.internal.InvocationMacroUtils
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

trait IsActivityImplicits {
  implicit def materialize[A]: IsActivity[A] =
    macro IsActivityImplicits.IsActivityMacro.materializeImpl[A]
}

private[zio] object IsActivityImplicits {
  class IsActivityMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
    import c.universe._

    def materializeImpl[A: WeakTypeTag]: Expr[IsActivity[A]] = {
      assertActivity(weakTypeOf[A].dealias, isFromImplicit = true)

      reify {
        IsActivity.__zio_temporal_IsActivityInstance.asInstanceOf[IsActivity[A]]
      }
    }
  }
}
