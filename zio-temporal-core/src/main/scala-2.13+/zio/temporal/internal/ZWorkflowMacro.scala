package zio.temporal.internal

import scala.reflect.macros.blackbox
import zio.temporal.ZWorkflowExecution

class ZWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val ZWorkflowExecution = typeOf[ZWorkflowExecution]

  def startImpl[A: WeakTypeTag](f: Expr[A]): Tree = {
    val invocation = getMethodInvocation(f.tree)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod("Workflow method should not be extension methods!")

    assertWorkflowMethod(method.symbol)
    val theStart = startInvocation(invocation, method, weakTypeOf[A])

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.from {
        new $ZWorkflowExecution(
          $theStart
        )
      } 
     """.debugged("Generated workflow method start")
  }

  def executeImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val theExecute = buildExecuteInvocation(f.tree, weakTypeOf[R])

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.fromFuture {
        $theExecute
      } 
     """.debugged("Generated workflow method start")
  }

  def executeEitherImpl[E: WeakTypeTag, R: WeakTypeTag](f: Expr[Either[E, R]]): Tree = {
    val theExecute = buildExecuteInvocation(f.tree, weakTypeOf[Either[E, R]])

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.fromFutureEither {
        $theExecute
      } 
     """.debugged("Generated workflow method start")
  }

  def asyncImpl[R: WeakTypeTag](f: Expr[R]): Tree =
    q"""
       _root_.zio.temporal.promise.ZPromise.fromEither(Right($f))
     """.debugged("Generated workflow start async")

  def asyncEitherImpl[E: WeakTypeTag, R: WeakTypeTag](f: Expr[Either[E, R]]): Tree =
    q"""
       new _root_.zio.temporal.promise.ZPromise.fromEither($f)
     """.debugged("Generated workflow start async")

  private def buildExecuteInvocation(f: Tree, ret: Type): Tree = {
    val invocation = getMethodInvocation(f)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod("Workflow method should not be extension methods!")

    assertWorkflowMethod(method.symbol)
    executeInvocation(invocation, method, ret)
  }

  private def startInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  ): Tree = {
    val LambdaConversionResult(tree, _, typeArgs, args) = scalaLambdaToFunction(invocation, method, ret)
    q"""_root_.io.temporal.client.WorkflowClient.start[..$typeArgs]($tree, ..$args)"""
  }

  private def executeInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  ): Tree = {
    val LambdaConversionResult(tree, _, typeArgs, args) = scalaLambdaToFunction(invocation, method, ret)
    q"""_root_.io.temporal.client.WorkflowClient.execute[..$typeArgs]($tree, ..$args)"""
  }

  private def assertWorkflowMethod(method: Symbol): Unit =
    getAnnotation(method, WorkflowMethod)
}
