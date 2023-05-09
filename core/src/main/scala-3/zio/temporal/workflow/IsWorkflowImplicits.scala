package zio.temporal.workflow

import zio.temporal.internal.InvocationMacroUtils
import scala.quoted.*

trait IsWorkflowImplicits {
  inline given materialize[A]: IsWorkflow[A] =
    ${ IsWorkflowImplicits.impl[A] }
}

object IsWorkflowImplicits {
  def impl[A: Type](using q: Quotes): Expr[IsWorkflow[A]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    macroUtils.assertWorkflow(TypeRepr.of[A], isFromImplicit = true)

    '{
      IsWorkflow.__zio_temporal_IsWorkflowInstance.asInstanceOf[IsWorkflow[A]]
    }
  }
}
