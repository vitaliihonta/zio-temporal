package zio.temporal.workflow

import zio.temporal.JavaTypeTag
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}

import scala.quoted._

trait ZChildWorkflowExecutionSyntax {
  inline def execute[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): R =
    ${ ZChildWorkflowExecutionSyntax.executeImpl[R]('f, 'javaTypeTag) }

  inline def executeAsync[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): ZAsync[R] =
    ${ ZChildWorkflowExecutionSyntax.executeAsyncImpl[R]('f, 'javaTypeTag) }
}

object ZChildWorkflowExecutionSyntax {
  def executeImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[R] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZChildWorkflowStub], "execute")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ChildWorkflowStub]

    '{
      TemporalWorkflowFacade.executeChild($stub, ${ method.argsExpr })($javaTypeTag)
    }.debugged(SharedCompileTimeMessages.generateChildWorkflowExecute)
  }

  def executeAsyncImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[ZAsync[R]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZChildWorkflowStub], "executeAsync")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ChildWorkflowStub]

    '{
      ZAsync.fromJava(
        TemporalWorkflowFacade.executeChildAsync($stub, ${ method.argsExpr })($javaTypeTag)
      )
    }.debugged(SharedCompileTimeMessages.generatedChildWorkflowExecuteAsync)
  }
}
