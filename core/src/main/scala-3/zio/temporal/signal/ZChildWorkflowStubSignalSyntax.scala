package zio.temporal.signal

import scala.quoted._
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import zio.temporal.workflow.ZChildWorkflowStub

trait ZChildWorkflowStubSignalSyntax {

  /** Sends a signal to the child workflow. Accepts the signal method invocation
    *
    * Example:
    * {{{
    *   val stub: ZChildWorkflowStub.Of[T] = ???
    *
    *   ZChildWorkflowStub.signal(
    *     stub.someSignalMethod(someArg)
    *   )
    * }}}
    *
    * @param f
    *   the signal method invocation
    */
  inline def signal(inline f: Unit): Unit =
    ${ ZChildWorkflowStubSignalSyntax.signalImpl('f) }
}

object ZChildWorkflowStubSignalSyntax {
  def signalImpl(f: Expr[Unit])(using q: Quotes): Expr[Unit] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._
    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZChildWorkflowStub], "signal")

    val method = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    method.assertSignalMethod()
    method.warnPossibleSerializationIssues()

    val signalName = getSignalName(method.symbol)

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ChildWorkflowStub]

    '{
      TemporalWorkflowFacade.signal($stub, ${ Expr(signalName) }, ${ method.argsExpr })
    }.debugged(SharedCompileTimeMessages.generatedSignal)
  }
}
