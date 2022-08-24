package zio.temporal.workflow

import zio.temporal.internal.{MacroUtils, SharedCompileTimeMessages}
import zio.temporal.internal.MacroUtils
import scala.quoted.*

trait HasPublicNullaryConstructorImplicits {
  inline given materialize[A]: HasPublicNullaryConstructor[A] =
    ${ HasPublicNullaryConstructorImplicits.impl[A] }
}

object HasPublicNullaryConstructorImplicits {
  def impl[A: Type](using q: Quotes): Expr[HasPublicNullaryConstructor[A]] = {
    import q.reflect.*
    val macroUtils = new MacroUtils[q.type]
    import macroUtils.*
    val A = TypeRepr.of[A]
    if (!hasPublicNullaryConstructor(A)) {
      error(SharedCompileTimeMessages.shouldHavePublicNullaryConstructor(A.show))
    }
    '{
      HasPublicNullaryConstructor.__zio_temporal_HasPublicNullaryConstructorInstance
        .asInstanceOf[HasPublicNullaryConstructor[A]]
    }
  }
}
