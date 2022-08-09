package zio.temporal.query

import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.internal.InvocationMacroUtils
import scala.quoted.*

// TODO: implement
trait ZWorkflowStubQuerySyntax {
  inline def query[R](inline f: R): TemporalIO[TemporalClientError, R] =
    ${ ZWorkflowStubQuerySyntax.queryImpl[R]('f) }

  inline def query[E, R](inline f: Either[E, R]): TemporalIO[TemporalError[E], R] =
    ${ ZWorkflowStubQuerySyntax.queryEitherImpl[E, R]('f) }
}

object ZWorkflowStubQuerySyntax {
  def queryImpl[R: Type](f: Expr[R])(using q: Quotes): Expr[TemporalIO[TemporalClientError, R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    val theQuery   = macroUtils.buildQueryInvocation(Expr.betaReduce(f).asTerm.underlying, TypeRepr.of[R])
    val result = '{
      _root_.zio.temporal.internal.TemporalInteraction.from[R] {
        ${ theQuery.asExprOf[R] }
      }
    }
    println(result.show)
    result
  }

  def queryEitherImpl[E: Type, R: Type](
    f:       Expr[Either[E, R]]
  )(using q: Quotes
  ): Expr[TemporalIO[TemporalError[E], R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    val theQuery   = macroUtils.buildQueryInvocation(Expr.betaReduce(f).asTerm.underlying, TypeRepr.of[Either[E, R]])
    val result = '{
      _root_.zio.temporal.internal.TemporalInteraction.fromEither[E, R] {
        ${
          theQuery.asExprOf[Either[E, R]]
        }
      }
    }
    println(result.show)
    result
  }
}
