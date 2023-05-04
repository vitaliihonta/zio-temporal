package zio.temporal.signal

import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.workflow.ZWorkflowStub

import scala.quoted.*
import zio.temporal.internal.{
  InvocationMacroUtils,
  SharedCompileTimeMessages,
  TemporalInteraction,
  TemporalWorkflowFacade
}

trait ZWorkflowStubSignalSyntax {
  inline def signal(inline f: Unit): TemporalIO[Unit] =
    ${ ZWorkflowStubSignalSyntax.signalImpl('f) }
}

trait ZWorkflowClientSignalWithStartSyntax { self: ZWorkflowStub =>

  /** Performs signal with start atomically.
    *
    * @param start
    *   workflow method call
    * @param signal
    *   signal method call
    * @return
    *   Workflow execution
    */
  inline def signalWithStart(inline start: Unit, inline signal: Unit): TemporalIO[ZWorkflowExecution] =
    ${ ZWorkflowStubSignalSyntax.signalWithStartImpl('self, 'start, 'signal) }
}

object ZWorkflowStubSignalSyntax {
  private val ZWorkflowStubType          = "ZWorkflowStub"
  private val TemporalWorkflowFacadeType = "TemporalWorkflowFacade"
  private val Init                       = "<init>"

  def signalImpl(f: Expr[Unit])(using q: Quotes): Expr[TemporalIO[Unit]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*
    val invocation = getMethodInvocationOfWorkflow(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, "ZWorkflowStub", "signal")

    val method = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    method.assertSignalMethod()
    val signalName = getSignalName(method.symbol)

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      TemporalInteraction.from {
        TemporalWorkflowFacade.signal($stub, ${ Expr(signalName) }, ${ method.argsExpr })
      }
    }.debugged(SharedCompileTimeMessages.generatedSignal)
  }

  def signalWithStartImpl(
    self:    Expr[ZWorkflowStub],
    start:   Expr[Unit],
    signal:  Expr[Unit]
  )(using q: Quotes
  ): Expr[TemporalIO[ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val startInvocation = getMethodInvocationOfWorkflow(start.asTerm)

    val startMethod = startInvocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    startMethod.assertWorkflowMethod()

    val signalInvocation = getMethodInvocationOfWorkflow(signal.asTerm)

    val signalMethod = signalInvocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    signalMethod.assertSignalMethod()
    val signalName = getSignalName(signalMethod.symbol)

    val startArgs = exprsToArray(
      startMethod.appliedArgs.map(_.asExprOf[Any])
    )

    val signalArgs = exprsToArray(
      signalMethod.appliedArgs.map(_.asExprOf[Any])
    )

    '{
      TemporalInteraction.from {
        new ZWorkflowExecution(
          TemporalWorkflowFacade.signalWithStart(
            $self.toJava,
            ${ Expr(signalName) },
            $signalArgs,
            $startArgs
          )
        )
      }
    }.debugged(SharedCompileTimeMessages.generatedSignalWithStart)
  }

  private def exprsToArray(xs: Seq[Expr[Any]])(using Quotes): Expr[Array[Any]] =
    if (xs.isEmpty) '{ Array.empty[Any] }
    else '{ Array(${ Expr.ofSeq(xs) }: _*) }
}
