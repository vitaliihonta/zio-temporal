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

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ChildWorkflowStub]

    '{
      TemporalWorkflowFacade.executeChild($stub, ${ method.argsExpr })($ctg)
    }.debugged(SharedCompileTimeMessages.generateChildWorkflowExecute)
  }

  def executeAsyncImpl[R: Type](f: Expr[R], ctg: Expr[ClassTag[R]])(using q: Quotes): Expr[ZAsync[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ChildWorkflowStub]

    '{
      ZAsync.fromJava(
        TemporalWorkflowFacade.executeChildAsync($stub, ${ method.argsExpr })($ctg)
      )
    }.debugged(SharedCompileTimeMessages.generatedChildWorkflowExecuteAsync)
  }
}
