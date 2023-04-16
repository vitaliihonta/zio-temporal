package zio.temporal.internal

import scala.reflect.macros.blackbox

class ZChildWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  def executeImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val invocation = getMethodInvocation(f.tree)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val executeInvocation = workflowExecuteInvocation(invocation, method, weakTypeOf[R])

    executeInvocation.debugged(SharedCompileTimeMessages.generateChildWorkflowExecute)
  }

  def executeAsyncImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val invocation = getMethodInvocation(f.tree)

    assertWorkflow(invocation.instance.tpe)

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
