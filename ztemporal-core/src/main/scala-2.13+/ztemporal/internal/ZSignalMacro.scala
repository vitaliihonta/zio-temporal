package ztemporal.internal

import io.temporal.workflow.Functions
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import izumi.reflect.Tag
import ztemporal.signal.ZInput
import ztemporal.signal.ZSignal
import ztemporal.utils.macros.MacroUtils

import scala.reflect.macros.blackbox

class ZSignalMacro(override val c: blackbox.Context) extends MacroUtils(c) {
  import c.universe._

  private val WorkflowInterface = typeOf[WorkflowInterface].dealias
  private val SignalMethod      = typeOf[SignalMethod].dealias
  private val WorkflowMethod    = typeOf[WorkflowMethod].dealias
  private val ZSignalMethod     = typeOf[ZSignal.SignalMethod]
  private val ZWorkflowMethod   = reify(ZSignal.WorkflowMethod)

  def signalImpl0(f: Expr[Unit]): Tree = {
    val (workflowTpe, methodName) = extractMethod0(f.tree).getOrElse(error(s"$f is not method selector"))

    val workflow   = assertWorkflow(workflowTpe)
    val signalName = getSignalName(workflow, methodName)

    val zSignalMethod = typeOf[ZSignal.SignalMethod]
    val zSignal       = weakTypeOf[ZSignal[Any, ZSignal.SignalMethod]]
    val signalFunc    = freshTermName("signalFunc")

    q"""
       val $signalFunc = () => $f
       new $zSignal(new $zSignalMethod($signalName), ( (_, batch) => batch.add(() => $signalFunc()) ))
       """ debugged s"Generated signal"
  }

  def signalImpl1[A: WeakTypeTag](f: Expr[A => Unit]): Tree = {
    val A = weakTypeOf[A].dealias

    val (workflowTpe, methodName) = extractMethod1(f.tree).getOrElse(error(s"$f is not method selector"))

    val workflow   = assertWorkflow(workflowTpe)
    val signalName = getSignalName(workflow, methodName)
    val tag        = getTag[A]
    val zSignal    = weakTypeOf[ZSignal[ZInput[A], ZSignal.SignalMethod]]
    val signalFunc = freshTermName("signalFunc")

    q"""
       val $signalFunc = $f
       new $zSignal(new $ZSignalMethod($signalName), ( (input, batch) => batch.add((a: $A) => $signalFunc(a), input.get($tag.tag).asInstanceOf[$A]) ))
       """ debugged s"Generated signal"
  }

  def signalImpl2[A: WeakTypeTag, B: WeakTypeTag](f: Expr[(A, B) => Unit]): Tree = {
    val A = weakTypeOf[A].dealias
    val B = weakTypeOf[B].dealias

    val (workflowTpe, methodName) = extractMethod2(f.tree).getOrElse(error(s"$f is not method selector"))

    val workflow   = assertWorkflow(workflowTpe)
    val signalName = getSignalName(workflow, methodName)

    val tagA = getTag[A]
    val tagB = getTag[B]

    val zSignal    = weakTypeOf[ZSignal[ZInput[A] with ZInput[B], ZSignal.SignalMethod]]
    val signalFunc = freshTermName("signalFunc")

    q"""
      val $signalFunc = $f
      new $zSignal(
        new $ZSignalMethod($signalName),
        (input, batch) =>
          batch.add(
            ((a: $A, b: $B) => $signalFunc(a, b)),
            input.get($tagA.tag).asInstanceOf[$A],
            input.get($tagB.tag).asInstanceOf[$B]
          )
      )
       """ debugged s"Generated signal"
  }

  def workflowMethodImpl0[A: WeakTypeTag](f: Expr[A]): Tree = {
    val Func = weakTypeOf[Functions.Func[A]]

    val (workflowTpe, methodName) = extractMethod0(f.tree).getOrElse(error(s"$f is not method selector"))

    val workflow = assertWorkflow(workflowTpe)
    assertWorkflowMethod(workflow, methodName)

    val zSignal      = weakTypeOf[ZSignal[Any, ZSignal.WorkflowMethod]]
    val workflowFunc = freshTermName("workflowFunc")

    q"""
       val $workflowFunc = $f
       new $zSignal($ZWorkflowMethod, ( (input, batch) => batch.add((() => $workflowFunc(a)): $Func) ))
       """ debugged s"Generated signal"
  }

  def workflowMethodImpl1[A: WeakTypeTag, B: WeakTypeTag](f: Expr[A => B]): Tree = {
    val A     = weakTypeOf[A].dealias
    val Func1 = weakTypeOf[Functions.Func1[A, B]]

    val (workflowTpe, methodName) = extractMethod1(f.tree).getOrElse(error(s"$f is not method selector"))

    val workflow = assertWorkflow(workflowTpe)
    assertWorkflowMethod(workflow, methodName)

    val tag          = getTag[A]
    val zSignal      = weakTypeOf[ZSignal[ZInput[A], ZSignal.WorkflowMethod]]
    val workflowFunc = freshTermName("workflowFunc")

    q"""
       val $workflowFunc = $f
       new $zSignal($ZWorkflowMethod, ( (input, batch) => batch.add(((a: $A) => $workflowFunc(a)): $Func1, input.get($tag.tag).asInstanceOf[$A]) ))
       """ debugged s"Generated signal"
  }

  def workflowMethodImpl2[A: WeakTypeTag, B: WeakTypeTag, C: WeakTypeTag](f: Expr[(A, B) => C]): Tree = {
    val A     = weakTypeOf[A].dealias
    val B     = weakTypeOf[B].dealias
    val Func2 = weakTypeOf[Functions.Func2[A, B, C]]

    val (workflowTpe, methodName) = extractMethod2(f.tree).getOrElse(error(s"$f is not method selector"))

    val workflow = assertWorkflow(workflowTpe)
    assertWorkflowMethod(workflow, methodName)

    val tagA = getTag[A]
    val tagB = getTag[B]

    val zSignal      = weakTypeOf[ZSignal[ZInput[A] with ZInput[B], ZSignal.WorkflowMethod]]
    val workflowFunc = freshTermName("workflowFunc")

    q"""
       val $workflowFunc = $f
       new $zSignal(
         $ZWorkflowMethod, ( (input, batch) => 
           batch.add(
             ((a: $A, b: $B) => $workflowFunc(a, b)): $Func2, 
             input.get($tagA.tag).asInstanceOf[$A],
             input.get($tagB.tag).asInstanceOf[$B]
           )
         )
       )
       """ debugged s"Generated signal"
  }

  private def getSignalName(workflow: Type, methodName: TermName): String =
    getMethodAnnotation(workflow, methodName, SignalMethod).children.tail
      .collectFirst {
        case NamedArg(_, Literal(Constant(signalName: String))) =>
          signalName
      }
      .getOrElse(methodName.toString)

  private def assertWorkflow(workflow: Type): Type = {
    def errorNotWorkflow = error(s"$workflow is not a workflow!")
    workflow match {
      case SingleType(_, sym) =>
        sym.typeSignature.baseClasses
          .flatMap(sym => findAnnotation(sym, WorkflowInterface).map(_ => sym.typeSignature))
          .headOption
          .getOrElse(
            errorNotWorkflow
          )

      case _ =>
        findAnnotation(workflow.typeSymbol, WorkflowInterface)
          .map(_ => workflow)
          .getOrElse(errorNotWorkflow)
    }
  }

  private def assertWorkflowMethod(workflow: Type, methodName: TermName): Unit =
    getMethodAnnotation(workflow, methodName, WorkflowMethod)

  private def getTag[A: WeakTypeTag] = {
    val tagTpe = weakTypeOf[Tag[A]]
    findImplicit(tagTpe, s"$tagTpe not found")
  }
}
