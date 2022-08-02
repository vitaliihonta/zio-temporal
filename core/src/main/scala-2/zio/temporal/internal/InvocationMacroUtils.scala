package zio.temporal.internal

import izumi.reflect.Tag
import zio.temporal.workflowInterface
import zio.temporal.workflowMethod
import scala.reflect.macros.blackbox

abstract class InvocationMacroUtils(override val c: blackbox.Context)
    extends MacroUtils(c)
    with VersionSpecificMacroUtils {
  import c.universe._

  protected val WorkflowInterface = typeOf[workflowInterface]
  protected val WorkflowMethod    = typeOf[workflowMethod]
  protected val UnitType          = typeOf[Unit].dealias

  protected case class MethodInfo(name: Name, symbol: Symbol, appliedArgs: List[Tree]) {
    validateCalls()

    private def validateCalls(): Unit =
      symbol.typeSignature.paramLists.headOption.foreach { expectedArgs =>
        appliedArgs.zip(expectedArgs).zipWithIndex.foreach { case ((actual, expected), argumentNo) =>
          if (!(actual.tpe <:< expected.typeSignature)) {
            error(
              s"Provided arguments for method $name doesn't confirm to it's signature:\n" +
                s"\tExpected: $expected (argument #${argumentNo + 1})\n" +
                s"\tGot: $actual (of type ${actual.tpe})"
            )
          }
        }
      }
  }

  protected case class MethodInvocation(instance: Ident, methodName: Name, args: List[Tree]) {
    def getMethod(errorDetails: => String): MethodInfo =
      instance.tpe.baseClasses
        .map(_.asClass.typeSignature.decl(methodName))
        .find(_ != NoSymbol)
        .map(MethodInfo(methodName, _, args))
        .getOrElse(
          error(s"${instance.tpe} doesn't have a $methodName method. " + errorDetails)
        )
  }

  protected def getMethodInvocation(tree: Tree): MethodInvocation =
    tree match {
      case Select(instance @ Ident(_), methodName) =>
        MethodInvocation(instance, methodName, Nil)
      case Apply(Select(instance @ Ident(_), methodName), args) =>
        MethodInvocation(instance, methodName, args)
      case _ => error(s"Expected simple method invocation, got tree of class ${tree.getClass}: $tree")
    }

  protected def getTag[A: WeakTypeTag] = {
    val tagTpe = weakTypeOf[Tag[A]]
    findImplicit(tagTpe, s"$tagTpe not found")
  }

  protected def assertWorkflow(workflow: Type): Type = {
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

  protected case class LambdaConversionResult(tree: Tree, typeAscription: Tree, typeArgs: List[Type], args: List[Tree])

  protected def scalaLambdaToFunction(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  ): LambdaConversionResult = {
    val f = q"""${invocation.instance}.${invocation.methodName.toTermName}"""
    method.appliedArgs match {
      case Nil =>
        val Proc = tq"""_root_.io.temporal.workflow.Functions.Proc"""
        val tree = q"""( () => $f() )"""
        LambdaConversionResult(tree, Proc, Nil, Nil)

      case args @ List(first) if ret =:= UnitType =>
        val a      = first.tpe.widen
        val aInput = freshTermName("a")
        val Proc1  = tq"""_root_.io.temporal.workflow.Functions.Proc1[$a]"""
        val tree   = q"""( ($aInput: $a) => $f($aInput) )"""
        LambdaConversionResult(tree, Proc1, List(a), args)

      case args @ List(first) =>
        val a      = first.tpe.widen
        val aInput = freshTermName("a")
        val Func1  = tq"""_root_.io.temporal.workflow.Functions.Func1[$a, $ret]"""
        val tree   = q"""( ($aInput: $a) => $f($aInput) )"""
        LambdaConversionResult(tree, Func1, List(a, ret), args)

      case args @ List(first, second) if ret =:= UnitType =>
        val a      = first.tpe.widen
        val b      = second.tpe.widen
        val aInput = freshTermName("a")
        val bInput = freshTermName("b")
        val Proc2  = tq"""_root_.io.temporal.workflow.Functions.Proc2[$a, $b]"""
        val tree   = q"""( ($aInput: $a, $bInput: $b) => $f($aInput, $bInput) )"""
        LambdaConversionResult(tree, Proc2, List(a, b), args)

      case args @ List(first, second) =>
        val a      = first.tpe.widen
        val b      = second.tpe.widen
        val aInput = freshTermName("a")
        val bInput = freshTermName("b")
        val Func2  = tq"""_root_.io.temporal.workflow.Functions.Func2[$a, $b, $ret]"""
        val tree   = q"""( ($aInput: $a, $bInput: $b) => $f($aInput, $bInput) )"""
        LambdaConversionResult(tree, Func2, List(a, b, ret), args)

      case args =>
        sys.error(s"Support for arity ${args.size} not currently implemented. Feel free to contribute!")
    }
  }
}
