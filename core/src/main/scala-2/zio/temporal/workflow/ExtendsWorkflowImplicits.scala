package zio.temporal.workflow

import zio.temporal.internal.InvocationMacroUtils
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

trait ExtendsWorkflowImplicits {
  implicit def materialize[A]: ExtendsWorkflow[A] =
    macro ExtendsWorkflowImplicits.ExtendsWorkflowMacro.materializeImpl[A]
}

private[zio] object ExtendsWorkflowImplicits {
  class ExtendsWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
    import c.universe._

    def materializeImpl[A: WeakTypeTag]: Expr[ExtendsWorkflow[A]] = {
      assertExtendsWorkflow(weakTypeOf[A].dealias)

      reify {
        ExtendsWorkflow.__zio_temporal_ExtendsWorkflowInstance.asInstanceOf[ExtendsWorkflow[A]]
      }
    }
  }
}
