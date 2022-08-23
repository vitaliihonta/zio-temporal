package zio.temporal.workflow

import scala.quoted.*

trait IsConcreteClassImplicits {
  inline given materialize[A]: IsConcreteClass[A] =
    ${ IsConcreteClassImplicits.impl[A] }
}

object IsConcreteClassImplicits {
  // TODO: implement properly
  def impl[A: Type](using q: Quotes): Expr[IsConcreteClass[A]] =
    '{ IsConcreteClass.__zio_temporal_IsConcreateClassInstance.asInstanceOf[IsConcreteClass[A]] }
}
