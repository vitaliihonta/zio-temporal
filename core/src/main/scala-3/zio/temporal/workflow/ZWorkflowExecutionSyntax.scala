package zio.temporal.workflow

import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import zio.temporal.promise.ZAsync

import scala.quoted.*

trait ZWorkflowExecutionSyntax {
  inline def start[A](inline f: A): TemporalIO[ZWorkflowExecution] =
    ${ ZWorkflowExecutionSyntax.startImpl[A]('f) }

  inline def execute[R](inline f: R): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeImpl[R]('f) }

  inline def async[R](inline f: R): ZAsync[R] =
    ${ ZWorkflowExecutionSyntax.asyncImpl[R]('f) }
}

object ZWorkflowExecutionSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[TemporalIO[ZWorkflowExecution]] = {
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

  def executeImpl[R: Type](f: Expr[R])(using q: Quotes): Expr[TemporalIO[R]] = {
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

  def asyncImpl[R: Type](f: Expr[R])(using q: Quotes): Expr[ZAsync[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    '{ ZAsync.attempt($fTree) }
      .debugged(SharedCompileTimeMessages.generatedWorkflowStartAsync)
  }
}
