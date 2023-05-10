package zio.temporal.workflow

import io.temporal.workflow.ContinueAsNewOptions
import zio.*
import zio.temporal.*
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}

import scala.quoted.*
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
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowContinueAsNewStub], "executeAsync")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val options = invocation.instance
      .select(invocation.instance.symbol.methodMember("options").head)
      .asExprOf[ContinueAsNewOptions]

    val workflowType = getWorkflowType(invocation.instance.tpe.widen)

    '{
      zio.temporal.internal.TemporalWorkflowFacade.continueAsNew[R](
        ${ Expr(workflowType) },
        $options,
        ${ method.argsExpr }
      )
    }.debugged(SharedCompileTimeMessages.generatedContinueAsNewExecute)
  }
}
