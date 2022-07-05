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

    val theStart = startInvocation(invocation, method, weakTypeOf[A])
    println(theStart)

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.from {
        new $ZWorkflowExecution(
          $theStart
        )
      } 
     """.debugged("Generated workflow method start")
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
      case _ => ???
    }
  }
}
