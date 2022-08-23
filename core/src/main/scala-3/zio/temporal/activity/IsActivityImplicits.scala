package zio.temporal.activity

import scala.quoted.*

trait IsActivityImplicits {
  inline given materialize[A]: IsActivity[A] =
    ${ IsActivityImplicits.impl[A] }
}

object IsActivityImplicits {
  // TODO: implement properly
  def impl[A: Type](using q: Quotes): Expr[IsActivity[A]] =
    '{ IsActivity.__zio_temporal_IsActivityInstance.asInstanceOf[IsActivity[A]] }
}
