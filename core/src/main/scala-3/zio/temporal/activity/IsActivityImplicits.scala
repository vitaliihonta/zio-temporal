package zio.temporal.activity

import zio.temporal.internal.InvocationMacroUtils
import scala.quoted._

trait IsActivityImplicits {
  inline given materialize[A]: IsActivity[A] =
    ${ IsActivityImplicits.impl[A] }
}

object IsActivityImplicits {
  def impl[A: Type](using q: Quotes): Expr[IsActivity[A]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    macroUtils.assertActivity(TypeRepr.of[A], isFromImplicit = true)
    '{ IsActivity.__zio_temporal_IsActivityInstance.asInstanceOf[IsActivity[A]] }
  }
}
