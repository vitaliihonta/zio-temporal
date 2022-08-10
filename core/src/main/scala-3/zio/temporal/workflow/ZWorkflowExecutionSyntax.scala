package zio.temporal.workflow

import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import zio.temporal.promise.ZPromise

import scala.quoted.*

trait ZWorkflowExecutionSyntax {
  inline def start[A](inline f: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    ${ ZWorkflowExecutionSyntax.startImpl[A]('f) }

  inline def execute[R](inline f: R): TemporalIO[TemporalClientError, R] =
    ${ ZWorkflowExecutionSyntax.executeImpl[R]('f) }

  inline def execute[E, R](inline f: Either[E, R]): TemporalIO[TemporalError[E], R] =
    ${ ZWorkflowExecutionSyntax.executeEitherImpl[E, R]('f) }

  inline def async[R](inline f: R): ZPromise[Nothing, R] =
    ${ ZWorkflowExecutionSyntax.asyncImpl[R]('f) }

  inline def async[E, R](inline f: Either[E, R]): ZPromise[E, R] =
    ${ ZWorkflowExecutionSyntax.asyncEitherImpl[E, R]('f) }
}

object ZWorkflowExecutionSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[TemporalIO[TemporalClientError, ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    '{
      zio.temporal.internal.TemporalInteraction.from {
        new ZWorkflowExecution(TemporalWorkflowFacade.start(() => $fTree))
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowStart)
  }

  def executeImpl[R: Type](f: Expr[R])(using q: Quotes): Expr[TemporalIO[TemporalClientError, R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        TemporalWorkflowFacade.execute(() => $fTree)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def executeEitherImpl[E: Type, R: Type](
    f:       Expr[Either[E, R]]
  )(using q: Quotes
  ): Expr[TemporalIO[TemporalError[E], R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    '{
      zio.temporal.internal.TemporalInteraction.fromFutureEither {
        TemporalWorkflowFacade.execute(() => $fTree)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def asyncImpl[R: Type](f: Expr[R])(using q: Quotes): Expr[ZPromise[Nothing, R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    '{ ZPromise.fromEither(Right($fTree)) }
      .debugged(SharedCompileTimeMessages.generatedWorkflowStartAsync)
  }

  def asyncEitherImpl[E: Type, R: Type](f: Expr[Either[E, R]])(using q: Quotes): Expr[ZPromise[E, R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    '{ ZPromise.fromEither($fTree) }
      .debugged(SharedCompileTimeMessages.generatedWorkflowStartAsync)
  }
}
