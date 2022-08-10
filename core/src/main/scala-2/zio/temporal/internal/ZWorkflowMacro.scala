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
    method.assertWorkflowMethod()

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.from {
        new $ZWorkflowExecution(
          _root_.zio.temporal.internal.TemporalWorkflowFacade.start(() => $f)
        )
      } 
     """.debugged("Generated workflow method start")
  }

  def executeImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val theExecute = buildExecuteInvocation(f.tree)

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.fromFuture {
        $theExecute
      } 
     """.debugged("Generated workflow method start")
  }

  def executeEitherImpl[E: WeakTypeTag, R: WeakTypeTag](f: Expr[Either[E, R]]): Tree = {
    val theExecute = buildExecuteInvocation(f.tree)

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

  private def buildExecuteInvocation(f: Tree): Tree = {
    val invocation = getMethodInvocation(f)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod("Workflow method should not be extension methods!")
    method.assertWorkflowMethod()

    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.execute(() => $f)"""
  }
}
