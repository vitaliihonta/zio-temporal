package zio.temporal.workflow

import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}

import scala.quoted.*
import scala.reflect.ClassTag

trait ZChildWorkflowExecutionSyntax {
  inline def execute[R](inline f: R)(using ctg: ClassTag[R]): R =
    ${ ZChildWorkflowExecutionSyntax.executeImpl[R]('f, 'ctg) }

  inline def executeAsync[R](inline f: R)(using ctg: ClassTag[R]): ZAsync[R] =
    ${ ZChildWorkflowExecutionSyntax.executeAsyncImpl[R]('f, 'ctg) }
}

object ZChildWorkflowExecutionSyntax {
  def executeImpl[R: Type](f: Expr[R], ctg: Expr[ClassTag[R]])(using q: Quotes): Expr[R] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocation(betaReduceExpression(f).asTerm)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.workflow.ChildWorkflowStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{
      TemporalWorkflowFacade.executeChild($stub, $castedArgs)($ctg)
    }.debugged(SharedCompileTimeMessages.generateChildWorkflowExecute)
  }

  def executeAsyncImpl[R: Type](f: Expr[R], ctg: Expr[ClassTag[R]])(using q: Quotes): Expr[ZAsync[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocation(betaReduceExpression(f).asTerm)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.workflow.ChildWorkflowStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{
      ZAsync.fromJava(
        TemporalWorkflowFacade.executeChildAsync($stub, $castedArgs)($ctg)
      )
    }.debugged(SharedCompileTimeMessages.generatedChildWorkflowExecuteAsync)
  }
}

// TODO: implement
trait ZExternalWorkflowExecutionSyntax {}
