package zio.temporal.workflow

import zio.temporal.internal.InvocationMacroUtils
import scala.reflect.macros.blackbox
import scala.language.experimental.macros

trait IsWorkflowImplicits {
  implicit def materialize[A]: IsWorkflow[A] =
    macro IsWorkflowImplicits.IsWorkflowMacro.materializeImpl[A]
}

private[zio] object IsWorkflowImplicits {
  class IsWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
    import c.universe._

    def materializeImpl[A: WeakTypeTag]: Expr[IsWorkflow[A]] = {
      assertExtendsWorkflow(weakTypeOf[A].dealias)

      reify {
        IsWorkflow.__zio_temporal_IsWorkflowInstance.asInstanceOf[IsWorkflow[A]]
      }
    }
  }
}
