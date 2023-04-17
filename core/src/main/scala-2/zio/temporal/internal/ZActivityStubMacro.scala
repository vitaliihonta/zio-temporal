package zio.temporal.internal

import zio.temporal.activity.ZActivityStub

import scala.reflect.macros.blackbox

class ZActivityStubMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val zactivityStub = typeOf[ZActivityStub.type].dealias

  def executeImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    // Assert called on ZActivityStub
    assertPrefixType(zactivityStub)

    val invocation = getMethodInvocation(f.tree)
    assertActivity(invocation.instance.tpe)

    val method       = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val activityName = getActivityName(method.symbol)

    val executeInvocation = activityExecuteInvocation(invocation, method, activityName, weakTypeOf[R])

    executeInvocation.debugged(SharedCompileTimeMessages.generatedActivityExecute)
  }

  def executeAsyncImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    // Assert called on ZActivityStub
    assertPrefixType(zactivityStub)

    val invocation = getMethodInvocation(f.tree)
    assertActivity(invocation.instance.tpe)

    val method       = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val activityName = getActivityName(method.symbol)

    val executeAsyncInvocation = activityExecuteAsyncInvocation(invocation, method, activityName, weakTypeOf[R])

    q"""
       _root_.zio.temporal.workflow.ZAsync.fromJava($executeAsyncInvocation)
     """.debugged(SharedCompileTimeMessages.generatedActivityExecuteAsync)
  }

  private def activityExecuteInvocation(
    invocation:   MethodInvocation,
    method:       MethodInfo,
    activityName: String,
    ret:          Type
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.executeActivity[$ret]($stub, $activityName, List(..${method.appliedArgs}))"""
  }

  private def activityExecuteAsyncInvocation(
    invocation:   MethodInvocation,
    method:       MethodInfo,
    activityName: String,
    ret:          Type
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.executeActivityAsync[$ret]($stub, $activityName, List(..${method.appliedArgs}))"""
  }
}
