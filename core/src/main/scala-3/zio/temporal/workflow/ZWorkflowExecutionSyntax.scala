package zio.temporal.workflow

import zio.*
import zio.temporal.*
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted.*

trait ZWorkflowExecutionSyntax {
  inline def start[A](inline f: A): TemporalIO[ZWorkflowExecution] =
    ${ ZWorkflowExecutionSyntax.startImpl[A]('f) }

  inline def execute[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeImpl[R]('f, 'javaTypeTag) }

  inline def executeWithTimeout[R](
    timeout:           Duration
  )(inline f:          R
  )(using javaTypeTag: JavaTypeTag[R]
  ): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeWithTimeoutImpl[R]('timeout, 'f, 'javaTypeTag) }
}

object ZWorkflowExecutionSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[TemporalIO[ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowStub], "start")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.from {
        new ZWorkflowExecution(
          TemporalWorkflowFacade.start($stub, ${ method.argsExpr })
        )
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowStart)
  }

  def executeImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[TemporalIO[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowStub], "execute")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        TemporalWorkflowFacade.execute($stub, ${ method.argsExpr })($javaTypeTag)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def executeWithTimeoutImpl[R: Type](
    timeout:     Expr[Duration],
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[TemporalIO[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowStub], "executeWithTimeout")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        TemporalWorkflowFacade.executeWithTimeout($stub, $timeout, ${ method.argsExpr })($javaTypeTag)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }
}
