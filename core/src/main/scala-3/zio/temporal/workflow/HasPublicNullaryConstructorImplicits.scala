package zio.temporal.workflow

import scala.quoted.*

trait HasPublicNullaryConstructorImplicits {
  inline given materialize[A]: HasPublicNullaryConstructor[A] =
    ${ HasPublicNullaryConstructorImplicits.impl[A] }
}

object HasPublicNullaryConstructorImplicits {
  // TODO: implement properly
  def impl[A: Type](using q: Quotes): Expr[HasPublicNullaryConstructor[A]] =
    '{
      HasPublicNullaryConstructor.__zio_temporal_HasPublicNullaryConstructorInstance
        .asInstanceOf[HasPublicNullaryConstructor[A]]
    }
}
