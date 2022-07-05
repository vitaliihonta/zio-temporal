package zio.temporal.internal

import izumi.reflect.Tag
import zio.temporal.utils.macros.MacroUtils
import zio.temporal.workflow
import zio.temporal.workflowMethod

import scala.reflect.macros.blackbox

abstract class InvocationMacroUtils(override val c: blackbox.Context) extends MacroUtils(c) {
  import c.universe._

  protected val WorkflowInterface = typeOf[workflow]
  protected val WorkflowMethod    = typeOf[workflowMethod]

  protected case class MethodInfo(name: Name, symbol: Symbol, appliedArgs: List[Tree]) {
    validateCalls()

    private def validateCalls(): Unit = {
      val expectedArgs = symbol.typeSignature.paramLists.head
      appliedArgs.zip(expectedArgs).zipWithIndex.foreach { case ((actual, expected), argumentNo) =>
        if (!(actual.tpe =:= expected.typeSignature)) {
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
      case Apply(Select(instance @ Ident(_), methodName), args) =>
        MethodInvocation(instance, methodName, args)
      case _ => error(error(s"Expected simple method invocation, got tree of class ${tree.getClass}: $tree"))
    }

  protected def getTag[A: WeakTypeTag] = {
    val tagTpe = weakTypeOf[Tag[A]]
    findImplicit(tagTpe, s"$tagTpe not found")
  }

  protected def assertWorkflowMethod(workflow: Type, methodName: TermName): Unit =
    getMethodAnnotation(workflow, methodName, WorkflowMethod)

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
