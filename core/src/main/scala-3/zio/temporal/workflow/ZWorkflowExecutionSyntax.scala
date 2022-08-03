package zio.temporal.workflow

import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.InvocationMacroUtils
import zio.temporal.promise.ZPromise

import scala.quoted.*

// TODO: implement
trait ZWorkflowExecutionSyntax {
  inline def start[A](f: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    ${ ZWorkflowExecutionSyntax.startImpl[A]('f) }

  inline def execute[R](f: R): TemporalIO[TemporalClientError, R] =
    ${ ZWorkflowExecutionSyntax.executeImpl[R]('f) }

  inline def execute[E, R](f: Either[E, R]): TemporalIO[TemporalError[E], R] =
    ${ ZWorkflowExecutionSyntax.executeEitherImpl[E, R]('f) }

  inline def async[R](f: R): ZPromise[Nothing, R] =
    ${ ZWorkflowExecutionSyntax.asyncImpl[R]('f) }

  inline def async[E, R](f: Either[E, R]): ZPromise[E, R] =
    ${ ZWorkflowExecutionSyntax.asyncEitherImpl[E, R]('f) }
}

object ZWorkflowExecutionSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[TemporalIO[TemporalClientError, ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    val invocation = macroUtils.getMethodInvocation(Expr.betaReduce(f).asTerm.underlying)
    val method     = invocation.getMethod("Workflow method should not be extension methods!")
    val theStart   = macroUtils.startInvocation(invocation, method, TypeRepr.of[A])
    val result = '{
      zio.temporal.internal.TemporalInteraction.from {
        new ZWorkflowExecution(${ theStart.asExprOf[io.temporal.api.common.v1.WorkflowExecution] })
      }
    }
    println(result.show)
    result
  }

  def executeImpl[R: Type](f: Expr[R])(using q: Quotes): Expr[TemporalIO[TemporalClientError, R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    val theExecute = macroUtils.buildExecuteInvocation(Expr.betaReduce(f).asTerm.underlying, TypeRepr.of[R])

    val result = '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        ${ theExecute.asExprOf[java.util.concurrent.CompletableFuture[R]] }
      }
    }
    println(result.show)
    result
  }

  def executeEitherImpl[E: Type, R: Type](
    f:       Expr[Either[E, R]]
  )(using q: Quotes
  ): Expr[TemporalIO[TemporalError[E], R]] = {
    import q.reflect.*
    ???
  }

  def asyncImpl[R: Type](f: Expr[R])(using q: Quotes): Expr[ZPromise[Nothing, R]] = {
    import q.reflect.*
    ???
  }

  def asyncEitherImpl[E: Type, R: Type](f: Expr[Either[E, R]])(using q: Quotes): Expr[ZPromise[E, R]] = {
    import q.reflect.*
    ???
  }
}
