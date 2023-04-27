package zio.temporal.workflow

import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages}
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

trait IsWorkflowInterfaceImplicits {
  implicit def materialize[A]: IsWorkflowInterface[A] =
    macro IsWorkflowInterfaceImplicits.IsWorkflowInterfaceMacro.materializeImpl[A]
}

private[zio] object IsWorkflowInterfaceImplicits {
  class IsWorkflowInterfaceMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
    import c.universe._

    def materializeImpl[A: WeakTypeTag]: Expr[IsWorkflowInterface[A]] = {
      val Res          = weakTypeOf[IsWorkflowInterface[A]].dealias
      val workflowType = getWorkflowType(weakTypeOf[A].dealias).debugged(SharedCompileTimeMessages.foundWorkflowType)

      c.Expr[IsWorkflowInterface[A]](
        q"""
           new _root_.zio.temporal.workflow.IsWorkflowInterface.__zio_temporal_IsWorkflowInterfaceInstance($workflowType)
             .asInstanceOf[$Res]
         """
      )
    }
  }
}
