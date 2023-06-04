package zio.temporal.activity

import zio.temporal.internal.InvocationMacroUtils
import scala.quoted._

trait ExtendsActivityImplicits {
  inline given materialize[A]: ExtendsActivity[A] =
    ${ ExtendsActivityImplicits.impl[A] }
}

object ExtendsActivityImplicits {
  def impl[A: Type](using q: Quotes): Expr[ExtendsActivity[A]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    macroUtils.assertExtendsActivity(TypeRepr.of[A])

    '{
      ExtendsActivity.__zio_temporal_ExtendsActivityInstance.asInstanceOf[ExtendsActivity[A]]
    }
  }
}
