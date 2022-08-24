package zio.temporal.internal

import scala.quoted.*

class MacroUtils[Q <: Quotes](using val q: Q) {
  import q.reflect.*

  def warning(message: String): Unit = report.warning(message)

  def error(message: String): Nothing = report.errorAndAbort(message)

  def companionObjectOf(tpe: TypeRepr): Ref =
    Ref(Symbol.requiredModule(tpe.show))

  private val nonConcreteClassFlags = List(Flags.Abstract, Flags.Trait)

  private val nonPublicFlags = List(Flags.Private, Flags.Protected, Flags.PrivateLocal)

  def isConcreteClass(tpe: TypeRepr): Boolean =
    tpe.classSymbol.exists(s => nonConcreteClassFlags.forall(!s.flags.is(_)))

  def assertConcreteClass(tpe: TypeRepr): Unit =
    if (!isConcreteClass(tpe)) {
      error(SharedCompileTimeMessages.isNotConcreteClass(tpe.show))
    }

  def hasPublicNullaryConstructor(tpe: TypeRepr): Boolean = {
    assertConcreteClass(tpe)
    tpe.classSymbol.get.declarations
      .exists(m => m.isClassConstructor && m.paramSymss.flatten.isEmpty && nonPublicFlags.forall(!m.flags.is(_)))
  }

  extension [A](self: Expr[A])
    def debugged(msg: String): Expr[A] = {
      if (debugEnabled) {
        report.info(s"$msg tree=${self.show}")
      }
      self
    }

  private lazy val debugEnabled: Boolean =
    sys.props
      .get("zio.temporal.debug.macro")
      .flatMap(str => scala.util.Try(str.toBoolean).toOption)
      .getOrElse(false)
}
