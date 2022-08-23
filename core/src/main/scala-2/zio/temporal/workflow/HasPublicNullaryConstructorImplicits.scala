package zio.temporal.workflow

import zio.temporal.internal.{MacroUtils, SharedCompileTimeMessages}

import scala.reflect.macros.blackbox
import scala.language.experimental.macros

trait HasPublicNullaryConstructorImplicits {
  implicit def materialize[A]: HasPublicNullaryConstructor[A] =
    macro HasPublicNullaryConstructorImplicits.HasPublicNullaryConstructorImplicitsMacro.materializeImpl[A]
}

private[zio] object HasPublicNullaryConstructorImplicits {
  class HasPublicNullaryConstructorImplicitsMacro(override val c: blackbox.Context) extends MacroUtils(c) {
    import c.universe._

    def materializeImpl[A: WeakTypeTag]: Expr[HasPublicNullaryConstructor[A]] = {
      val A = weakTypeOf[A].dealias
      if (!hasPublicNullaryConstructor(A)) {
        error(SharedCompileTimeMessages.shouldHavePublicNullaryConstructor(A.toString))
      }

      reify {
        HasPublicNullaryConstructor.__zio_temporal_HasPublicNullaryConstructorInstance
          .asInstanceOf[HasPublicNullaryConstructor[A]]
      }
    }
  }
}
