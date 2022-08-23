package zio.temporal.workflow

import scala.quoted.*

trait IsWorkflowImplicits {
  inline given materialize[A]: IsWorkflow[A] =
    ${ IsWorkflowImplicits.impl[A] }
}

object IsWorkflowImplicits {
  // TODO: implement properly
  def impl[A: Type](using q: Quotes): Expr[IsWorkflow[A]] =
    '{ IsWorkflow.__zio_temporal_IsWorkflowInstance.asInstanceOf[IsWorkflow[A]] }
}
