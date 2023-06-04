package zio.temporal.workflow

import zio.temporal.internal.InvocationMacroUtils
import scala.quoted._

trait ExtendsWorkflowImplicits {
  inline given materialize[A]: ExtendsWorkflow[A] =
    ${ ExtendsWorkflowImplicits.impl[A] }
}

object ExtendsWorkflowImplicits {
  def impl[A: Type](using q: Quotes): Expr[ExtendsWorkflow[A]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    macroUtils.assertExtendsWorkflow(TypeRepr.of[A])

    '{
      ExtendsWorkflow.__zio_temporal_ExtendsWorkflowInstance.asInstanceOf[ExtendsWorkflow[A]]
    }
  }
}
