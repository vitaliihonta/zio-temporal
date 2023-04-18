package zio.temporal.workflow

import zio.*
import zio.temporal.*
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted.*
import scala.reflect.ClassTag

trait ZWorkflowExecutionSyntax {
  inline def start[A](inline f: A): TemporalIO[ZWorkflowExecution] =
    ${ ZWorkflowExecutionSyntax.startImpl[A]('f) }

  inline def execute[R](inline f: R)(using ctg: ClassTag[R]): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeImpl[R]('f, 'ctg) }

  inline def executeWithTimeout[R](timeout: Duration)(inline f: R)(using ctg: ClassTag[R]): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeWithTimeoutImpl[R]('timeout, 'f, 'ctg) }
}

object ZWorkflowExecutionSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[TemporalIO[ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)

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
    f:       Expr[R],
    ctg:     Expr[ClassTag[R]]
  )(using q: Quotes
  ): Expr[TemporalIO[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        TemporalWorkflowFacade.execute($stub, ${ method.argsExpr })($ctg)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def executeWithTimeoutImpl[R: Type](
    timeout: Expr[Duration],
    f:       Expr[R],
    ctg:     Expr[ClassTag[R]]
  )(using q: Quotes
  ): Expr[TemporalIO[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        TemporalWorkflowFacade.executeWithTimeout($stub, $timeout, ${ method.argsExpr })($ctg)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }
}
