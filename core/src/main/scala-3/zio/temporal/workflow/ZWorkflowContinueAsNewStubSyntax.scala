package zio.temporal.workflow

import io.temporal.workflow.ContinueAsNewOptions
import zio._
import zio.temporal._
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}

import scala.quoted._
import scala.reflect.ClassTag

trait ZWorkflowContinueAsNewStubSyntax {
  inline def execute[R](inline f: R): R =
    ${ ZWorkflowContinueAsNewStubSyntax.executeImpl[R]('f) }
}

object ZWorkflowContinueAsNewStubSyntax {
  def executeImpl[R: Type](
    f:       Expr[R]
  )(using q: Quotes
  ): Expr[R] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowContinueAsNewStub], "executeAsync")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    val workflowType = invocation.instance
      .select(invocation.instance.symbol.methodMember("workflowType").head)
      .asExprOf[String]

    val options = invocation.instance
      .select(invocation.instance.symbol.methodMember("options").head)
      .asExprOf[ContinueAsNewOptions]

    '{
      zio.temporal.internal.TemporalWorkflowFacade.continueAsNew[R](
        $workflowType,
        $options,
        ${ method.argsExpr }
      )
    }.debugged(SharedCompileTimeMessages.generatedContinueAsNewExecute)
  }
}
