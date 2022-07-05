package zio.temporal.internal

import zio.temporal._
import zio.temporal.signal.ZSignal
import scala.reflect.macros.blackbox

class ZSignalMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val SignalMethod = typeOf[signalMethod]

  private val Signal          = typeOf[ZSignal.Signal].dealias
  private val SignalWithStart = typeOf[ZSignal.WithStart].dealias

  def startImpl[A](f: Expr[A]): Tree = {
    println(f)
    println(f.getClass)
    ???
  }

  def signalImpl(f: Expr[Unit]): Tree = {
    val invocation = getMethodInvocation(f.tree)

    assertWorkflow(invocation.instance.tpe)
    if (!(invocation.instance.tpe <:< typeOf[BaseCanSignal])) {
      error(s".signal should be called only on ZWorkflowStub and etc.")
    }

    val method = invocation.getMethod("Signal method should not be extension methods!")

    val signalName = getSignalName(method.symbol)

    q"""${invocation.instance}.__zio_temporal_invokeSignal(new $Signal($signalName, ${invocation.args}))""".debugged(
      "Generated signal"
    )
  }

  def signalWithStartImpl[A](f: Expr[A]): Tree = {
    println(f.tree)
    println(f.tree.getClass)
    ???
  }
//
//  def workflowMethodImpl0[A: WeakTypeTag](f: Expr[A]): Tree = {
//    val Func = weakTypeOf[Functions.Func[A]]
//
//    val (workflowTpe, methodName) = extractMethod0(f.tree).getOrElse(error(s"$f is not method selector"))
//
//    val workflow = assertWorkflow(workflowTpe)
//    assertWorkflowMethod(workflow, methodName)
//
//    val zSignal      = weakTypeOf[ZSignal[Any, ZSignal.WorkflowMethod]]
//    val workflowFunc = freshTermName("workflowFunc")
//
//    q"""
//       val $workflowFunc = $f
//       new $zSignal($ZWorkflowMethod, ( (input, batch) => batch.add((() => $workflowFunc(a)): $Func) ))
//       """ debugged s"Generated signal"
//  }
//
//  def workflowMethodImpl1[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B]): Tree = {
//    val A     = weakTypeOf[A].dealias
//    val Func1 = weakTypeOf[Functions.Func1[A, B]]
//
//    val (workflowTpe, methodName) = extractMethod1(f.tree).getOrElse(error(s"$f is not method selector"))
//
//    val workflow = assertWorkflow(workflowTpe)
//    assertWorkflowMethod(workflow, methodName)
//
//    val tag          = getTag[A]
//    val zSignal      = weakTypeOf[ZSignal[ZInput[A], ZSignal.WorkflowMethod]]
//    val workflowFunc = freshTermName("workflowFunc")
//
//    q"""
//       val $workflowFunc = $f
//       new $zSignal($ZWorkflowMethod, ( (input, batch) => batch.add(((a: $A) => $workflowFunc(a)): $Func1, input.get($tag.tag).asInstanceOf[$A]) ))
//       """ debugged s"Generated signal"
//  }
//
//  def workflowMethodImpl2[A: WeakTypeTag, B: WeakTypeTag, C: WeakTypeTag](f: Expr[(A, B) => C]): Tree = {
//    val A     = weakTypeOf[A].dealias
//    val B     = weakTypeOf[B].dealias
//    val Func2 = weakTypeOf[Functions.Func2[A, B, C]]
//
//    val (workflowTpe, methodName) = extractMethod2(f.tree).getOrElse(error(s"$f is not method selector"))
//
//    val workflow = assertWorkflow(workflowTpe)
//    assertWorkflowMethod(workflow, methodName)
//
//    val tagA = getTag[A]
//    val tagB = getTag[B]
//
//    val zSignal      = weakTypeOf[ZSignal[ZInput[A] with ZInput[B], ZSignal.WorkflowMethod]]
//    val workflowFunc = freshTermName("workflowFunc")
//
//    q"""
//       val $workflowFunc = $f
//       new $zSignal(
//         $ZWorkflowMethod, ( (input, batch) =>
//           batch.add(
//             ((a: $A, b: $B) => $workflowFunc(a, b)): $Func2,
//             input.get($tagA.tag).asInstanceOf[$A],
//             input.get($tagB.tag).asInstanceOf[$B]
//           )
//         )
//       )
//       """ debugged s"Generated signal"
//  }

  private def getSignalName(method: Symbol): String =
    getAnnotation(method, SignalMethod).children.tail
      .collectFirst { case NamedArg(_, Literal(Constant(signalName: String))) =>
        signalName
      }
      .getOrElse(method.name.toString)
}
