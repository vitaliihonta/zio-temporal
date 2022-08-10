package zio.temporal.internal

import izumi.reflect.Tag
import zio.temporal.*
import scala.reflect.macros.blackbox

abstract class InvocationMacroUtils(override val c: blackbox.Context)
    extends MacroUtils(c)
    with VersionSpecificMacroUtils {
  import c.universe._

  protected val WorkflowInterface = typeOf[workflowInterface].dealias
  protected val WorkflowMethod    = typeOf[workflowMethod].dealias
  protected val QueryMethod       = typeOf[queryMethod].dealias
  protected val SignalMethod      = typeOf[signalMethod].dealias

  protected case class MethodInfo(name: Name, symbol: Symbol, appliedArgs: List[Tree]) {
    validateCalls()

    def assertWorkflowMethod(): Unit =
      if (!hasAnnotation(symbol, WorkflowMethod)) {
        sys.error(s"The method is not a @workflowMethod: $symbol")
      }

    def assertSignalMethod(): Unit =
      if (!hasAnnotation(symbol, SignalMethod)) {
        sys.error(s"The method is not a @signalMethod: $symbol")
      }

    def assertQueryMethod(): Unit =
      if (!hasAnnotation(symbol, QueryMethod)) {
        sys.error(s"The method is not a @queryMethod: $symbol")
      }

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

  protected case class MethodInvocation(instance: Tree, methodName: Name, args: List[Tree]) {

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
      case Select(instance, methodName) =>
        MethodInvocation(instance, methodName, Nil)
      case Apply(Select(instance, methodName), args) =>
        MethodInvocation(instance, methodName, args)
      case TypeApply(inner, _) =>
        getMethodInvocation(inner)
      case Block(List(inner), _) =>
        getMethodInvocation(inner)
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
}
