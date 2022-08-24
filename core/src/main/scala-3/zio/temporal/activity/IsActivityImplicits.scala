package zio.temporal.activity

import zio.temporal.internal.InvocationMacroUtils
import scala.quoted.*

trait IsActivityImplicits {
  inline given materialize[A]: IsActivity[A] =
    ${ IsActivityImplicits.impl[A] }
}

object IsActivityImplicits {
  def impl[A: Type](using q: Quotes): Expr[IsActivity[A]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    macroUtils.assertExtendsActivity(TypeRepr.of[A])
    '{ IsActivity.__zio_temporal_IsActivityInstance.asInstanceOf[IsActivity[A]] }
  }
}
