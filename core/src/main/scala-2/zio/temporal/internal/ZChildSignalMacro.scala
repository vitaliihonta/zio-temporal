package zio.temporal.internal

import zio.temporal.workflow.ZChildWorkflowStub
import scala.reflect.macros.blackbox

class ZChildSignalMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val zchildWorkflowStub = typeOf[ZChildWorkflowStub.type].dealias

  def signalImpl(f: Expr[Unit]): Tree = {
    // Assert called on ZChildWorkflowStub
    assertPrefixType(zchildWorkflowStub)

    val tree       = f.tree
    val invocation = getMethodInvocation(tree)
    assertTypedWorkflowStub(invocation.instance.tpe, typeOf[ZChildWorkflowStub], "signal")

    val method = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    method.assertSignalMethod()
    val signalName = getSignalName(method.symbol)

    q"""
       _root_.zio.temporal.internal.TemporalWorkflowFacade.signal(
         ${invocation.instance}.toJava,
         $signalName,
         ${invocation.args}
       )
    """.debugged(SharedCompileTimeMessages.generatedSignal)
  }
}
