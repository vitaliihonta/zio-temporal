package zio.temporal.workflow

import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages}
import scala.quoted.*
import scala.reflect.ClassTag

trait ZWorkflowExecutionSyntax {
  inline def start[A](inline f: A): TemporalIO[ZWorkflowExecution] =
    ${ ZWorkflowExecutionSyntax.startImpl[A]('f) }

  inline def execute[R](inline f: R)(using ctg: ClassTag[R]): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeImpl[R]('f, 'ctg) }
}

object ZWorkflowExecutionSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[TemporalIO[ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val theStart = buildStartWorkflowInvocation(betaReduceExpression(f).asTerm)

    '{
      zio.temporal.internal.TemporalInteraction.from {
        new ZWorkflowExecution(
          $theStart
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

    val theExecute = buildExecuteWorkflowInvocation(betaReduceExpression(f).asTerm, ctg)

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        $theExecute
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }
}
