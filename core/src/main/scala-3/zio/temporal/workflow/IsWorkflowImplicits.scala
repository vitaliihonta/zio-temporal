package zio.temporal.workflow

import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages}

import scala.quoted.*

trait IsWorkflowImplicits {
  inline given materialize[A]: IsWorkflow[A] =
    ${ IsWorkflowImplicits.impl[A] }
}

object IsWorkflowImplicits {
  def impl[A: Type](using q: Quotes): Expr[IsWorkflow[A]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    macroUtils.assertExtendsWorkflow(TypeRepr.of[A])
    '{ IsWorkflow.__zio_temporal_IsWorkflowInstance.asInstanceOf[IsWorkflow[A]] }
  }
}

trait IsWorkflowInterfaceImplicits {
  inline given materialize[A]: IsWorkflowInterface[A] =
    ${ IsWorkflowInterfaceImplicits.impl[A] }
}

object IsWorkflowInterfaceImplicits {
  def impl[A: Type](using q: Quotes): Expr[IsWorkflowInterface[A]] = {
    import q.reflect.*
    val macroUtils   = new InvocationMacroUtils[q.type]
    val workflowType = macroUtils.getWorkflowType(TypeRepr.of[A])
    report.info(SharedCompileTimeMessages.foundWorkflowType + s" tree=$workflowType")

    '{
      new IsWorkflowInterface.__zio_temporal_IsWorkflowInterfaceInstance(${ Expr(workflowType) })
        .asInstanceOf[IsWorkflowInterface[A]]
    }
  }
}
