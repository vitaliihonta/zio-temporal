package zio.temporal.signal

import scala.quoted.*
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}

trait ZChildWorkflowStubSignalSyntax {
  inline def signal(inline f: Unit): Unit =
    ${ ZChildWorkflowStubSignalSyntax.signalImpl('f) }
}

object ZChildWorkflowStubSignalSyntax {
  def signalImpl(f: Expr[Unit])(using q: Quotes): Expr[Unit] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*
    val invocation = getMethodInvocation(f.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)

    method.assertSignalMethod()
    val signalName = getSignalName(method.symbol)

    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.workflow.ChildWorkflowStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{
      TemporalWorkflowFacade.signal($stub, ${ Expr(signalName) }, $castedArgs)
    }.debugged(SharedCompileTimeMessages.generatedSignal)
  }
}
