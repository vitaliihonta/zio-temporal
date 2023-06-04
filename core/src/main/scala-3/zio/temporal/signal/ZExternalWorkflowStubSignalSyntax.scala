package zio.temporal.signal

import scala.quoted._
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import zio.temporal.workflow.ZExternalWorkflowStub

trait ZExternalWorkflowStubSignalSyntax {
  inline def signal(inline f: Unit): Unit =
    ${ ZExternalWorkflowStubSignalSyntax.signalImpl('f) }
}

object ZExternalWorkflowStubSignalSyntax {
  def signalImpl(f: Expr[Unit])(using q: Quotes): Expr[Unit] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._
    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZExternalWorkflowStub], "signal")

    val method = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    method.assertSignalMethod()
    method.warnPossibleSerializationIssues()

    val signalName = getSignalName(method.symbol)

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ExternalWorkflowStub]

    '{
      TemporalWorkflowFacade.signal($stub, ${ Expr(signalName) }, ${ method.argsExpr })
    }.debugged(SharedCompileTimeMessages.generatedSignal)
  }
}
