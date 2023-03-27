package zio.temporal.internal

import scala.reflect.macros.blackbox
import zio.temporal.ZWorkflowExecution

class ZWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val ZWorkflowExecution = typeOf[ZWorkflowExecution]

  def startImpl[A: WeakTypeTag](f: Expr[A]): Tree = {
    val invocation = getMethodInvocation(f.tree)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.from {
        new $ZWorkflowExecution(
          _root_.zio.temporal.internal.TemporalWorkflowFacade.start(() => $f)
        )
      } 
     """.debugged(SharedCompileTimeMessages.generatedWorkflowStart)
  }

  def executeImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val theExecute = buildExecuteInvocation(f.tree)

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.fromFuture {
        $theExecute
      } 
     """.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def asyncImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val invocation = getMethodInvocation(f.tree)
    val method     = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    q"""
       _root_.zio.temporal.promise.ZAsync.attempt($f)
     """.debugged(SharedCompileTimeMessages.generatedWorkflowStartAsync)
  }

  private def buildExecuteInvocation(f: Tree): Tree = {
    val invocation = getMethodInvocation(f)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.execute(() => $f)"""
  }
}
