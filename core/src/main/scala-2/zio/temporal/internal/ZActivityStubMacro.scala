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
    assertTypedActivityStub(invocation.instance.tpe, "execute")

    val method     = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val methodName = method.symbol.name.toString

    val executeInvocation = activityExecuteInvocation(invocation, method, methodName, weakTypeOf[R])

    executeInvocation.debugged(SharedCompileTimeMessages.generatedActivityExecute)
  }

  def executeAsyncImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    // Assert called on ZActivityStub
    assertPrefixType(zactivityStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedActivityStub(invocation.instance.tpe, "executeAsync")

    val method     = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val methodName = method.symbol.name.toString

    val executeAsyncInvocation = activityExecuteAsyncInvocation(invocation, method, methodName, weakTypeOf[R])

    q"""
       _root_.zio.temporal.workflow.ZAsync.fromJava($executeAsyncInvocation)
     """.debugged(SharedCompileTimeMessages.generatedActivityExecuteAsync)
  }

  private def activityExecuteInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    methodName: String,
    ret:        Type
  ): Tree = {
    val stub         = q"""${invocation.instance}.toJava"""
    val stubbedClass = q"""${invocation.instance}.stubbedClass"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.executeActivity[$ret]($stub, $stubbedClass, $methodName, List(..${method.appliedArgs}))"""
  }

  private def activityExecuteAsyncInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    methodName: String,
    ret:        Type
  ): Tree = {
    val stub         = q"""${invocation.instance}.toJava"""
    val stubbedClass = q"""${invocation.instance}.stubbedClass"""
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.executeActivityAsync[$ret]($stub, $stubbedClass, $methodName, List(..${method.appliedArgs}))"""
  }
}
