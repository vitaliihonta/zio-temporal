package zio.temporal.internal

import zio.Duration

import scala.reflect.macros.blackbox
import zio.temporal.ZWorkflowExecution
import zio.temporal.workflow.{ZWorkflowContinueAsNewStub, ZWorkflowStub}

class ZWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val ZWorkflowExecution         = typeOf[ZWorkflowExecution]
  private val zworkflowStub              = typeOf[ZWorkflowStub.type].dealias
  private val zworkflowContinueAsNewStub = typeOf[ZWorkflowContinueAsNewStub.type].dealias

  def startImpl[A: WeakTypeTag](f: Expr[A]): Tree = {
    // Assert called on ZWorkflowStub
    assertPrefixType(zworkflowStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedWorkflowStub(invocation.instance.tpe, "ZWorkflowStub", "start")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val startInvocation = workflowStartInvocation(invocation, method)

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.from {
        new $ZWorkflowExecution(
          $startInvocation
        )
      } 
     """.debugged(SharedCompileTimeMessages.generatedWorkflowStart)
  }

  def executeImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    // Assert called on ZWorkflowStub
    assertPrefixType(zworkflowStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedWorkflowStub(invocation.instance.tpe, "ZWorkflowStub", "execute")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val executeInvocation = workflowExecuteInvocation(invocation, method, weakTypeOf[R])

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.fromFuture {
        $executeInvocation
      } 
     """.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def executeWithTimeoutImpl[R: WeakTypeTag](timeout: Expr[Duration])(f: Expr[R]): Tree = {
    // Assert called on ZWorkflowStub
    assertPrefixType(zworkflowStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedWorkflowStub(invocation.instance.tpe, "ZWorkflowStub", "executeWithTimeout")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val executeInvocation = workflowExecuteWithTimeoutInvocation(invocation, method, timeout, weakTypeOf[R])

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.fromFuture {
        $executeInvocation
      } 
     """.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def continueAsNewImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    // Assert called on ZWorkflowContinueAsNewStub
    assertPrefixType(zworkflowContinueAsNewStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedWorkflowStub(invocation.instance.tpe, "ZWorkflowContinueAsNewStub", "execute")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    val workflowType = getWorkflowType(invocation.instance.tpe)

    val ret = weakTypeOf[R]

    val options = q"""${invocation.instance}.options"""
    q"""
      _root_.zio.temporal.internal.TemporalWorkflowFacade.continueAsNew[$ret]($workflowType, $options, List(..${method.appliedArgs}))
     """.debugged(SharedCompileTimeMessages.generatedContinueAsNewExecute)
  }

  private def workflowStartInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.start($stub, List(..${method.appliedArgs}))"""
  }

  private def workflowExecuteInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.execute[$ret]($stub, List(..${method.appliedArgs}))"""
  }

  private def workflowExecuteWithTimeoutInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    timeout:    Expr[Duration],
    ret:        Type
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.executeWithTimeout[$ret]($stub, $timeout, List(..${method.appliedArgs}))"""
  }
}
