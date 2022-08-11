package zio.temporal.internal

import scala.quoted.*

class MacroUtils[Q <: Quotes](using val q: Q) {
  import q.reflect.*

  def warning(message: String): Unit = report.warning(message)

  def error(message: String): Nothing = report.errorAndAbort(message)

  def companionObjectOf(tpe: TypeRepr): Ref =
    Ref(Symbol.requiredModule(tpe.show))

  extension [A](self: Expr[A])
    def debugged(msg: String): Expr[A] = {
      if (debugEnabled) {
        report.info(s"$msg tree=${self.show}")
      }
      self
    }

  private def debugEnabled: Boolean =
    sys.props
      .get("zio.temporal.debug.macro")
      .flatMap(str => scala.util.Try(str.toBoolean).toOption)
      .getOrElse(false)
}
