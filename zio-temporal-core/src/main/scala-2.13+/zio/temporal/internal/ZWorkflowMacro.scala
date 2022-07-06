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
    val f = q"""${invocation.instance}.${invocation.methodName.toTermName}"""
    method.appliedArgs match {
      case Nil =>
        q"""_root_.io.temporal.client.WorkflowClient.start[$ret]((() => $f()))"""
      case List(first) =>
        val a      = first.tpe
        val aInput = freshTermName("a")
        q"""_root_.io.temporal.client.WorkflowClient.start[$a, $ret]((($aInput: $a) => $f($aInput)), $first)"""
      case List(first, second) =>
        val a      = first.tpe
        val b      = second.tpe
        val aInput = freshTermName("a")
        val bInput = freshTermName("b")
        q"""_root_.io.temporal.client.WorkflowClient.start[$a, $b, $ret]((($aInput: $a, $bInput: $b) => $f($aInput, $aInput)), $first, $second)"""
      case args =>
        sys.error(s"Workflow start with arity ${args.size} not currently implemented. Feel free to contribute!")
    }
  }

  private def executeInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  ): Tree = {
    val f = q"""${invocation.instance}.${invocation.methodName.toTermName}"""
    method.appliedArgs match {
      case Nil =>
        q"""_root_.io.temporal.client.WorkflowClient.execute[$ret]((() => $f()))"""
      case List(first) =>
        val a      = first.tpe
        val aInput = freshTermName("a")
        val Func1  = tq"""io.temporal.workflow.Functions.Func1[$a, $ret]"""
        q"""_root_.io.temporal.client.WorkflowClient.execute[$a, $ret]( ( ($aInput: $a) => $f($aInput) ): $Func1, $first )"""
      case List(first, second) =>
        val a      = first.tpe
        val b      = second.tpe
        val aInput = freshTermName("a")
        val bInput = freshTermName("b")
        val Func2  = tq"""io.temporal.workflow.Functions.Func2[$a, $b, $ret]"""
        q"""_root_.io.temporal.client.WorkflowClient.execute[$a, $b, $ret]( ( ($aInput: $a, $bInput: $b) => $f($aInput, $bInput) ): $Func2, $first, $second)"""
      case args =>
        sys.error(s"Workflow execute with arity ${args.size} not currently implemented. Feel free to contribute!")
    }
  }

  private def assertWorkflowMethod(method: Symbol): Unit =
    getAnnotation(method, WorkflowMethod)
}
