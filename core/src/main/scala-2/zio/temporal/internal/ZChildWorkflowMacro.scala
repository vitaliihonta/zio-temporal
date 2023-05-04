package zio.temporal.internal

import zio.temporal.workflow.ZChildWorkflowStub
import scala.reflect.macros.blackbox

class ZChildWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val zchildWorkflowStub = typeOf[ZChildWorkflowStub.type].dealias

  def executeImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    // Assert called on ZChildWorkflowStub
    assertPrefixType(zchildWorkflowStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedWorkflowStub(invocation.instance.tpe, "ZChildWorkflowStub", "execute")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val executeInvocation = workflowExecuteInvocation(invocation, method, weakTypeOf[R])

    executeInvocation.debugged(SharedCompileTimeMessages.generateChildWorkflowExecute)
  }

  def executeAsyncImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    // Assert called on ZChildWorkflowStub
    assertPrefixType(zchildWorkflowStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedWorkflowStub(invocation.instance.tpe, "ZChildWorkflowStub", "executeAsync")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val executeAsyncInvocation = workflowExecuteAsyncInvocation(invocation, method, weakTypeOf[R])

    q"""
       _root_.zio.temporal.workflow.ZAsync.fromJava($executeAsyncInvocation)
     """.debugged(SharedCompileTimeMessages.generatedChildWorkflowExecuteAsync)
  }

  private def workflowExecuteInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.executeChild[$ret]($stub, List(..${method.appliedArgs}))"""
  }

  private def workflowExecuteAsyncInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.executeChildAsync[$ret]($stub, List(..${method.appliedArgs}))"""
  }
}
