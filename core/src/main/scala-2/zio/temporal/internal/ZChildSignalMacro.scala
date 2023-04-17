package zio.temporal.internal

import zio.temporal.workflow.ZChildWorkflowStub
import scala.reflect.macros.blackbox

class ZChildSignalMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val zchildWorkflowStub = typeOf[ZChildWorkflowStub.type].dealias

  def signalImpl(f: Expr[Unit]): Tree = {
    // Assert called on ZChildWorkflowStub
    val _ = getPrefixOf(zchildWorkflowStub)

    val tree       = f.tree
    val invocation = getMethodInvocation(tree)
    assertWorkflow(invocation.instance.tpe)

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
