package zio.temporal.internal

import zio.temporal.*
import scala.quoted.*

class InvocationMacroUtils[Q <: Quotes](using val q: Q) {
  import q.reflect.*

  private val WorkflowInterface = TypeRepr.of[workflowInterface].typeSymbol

  def getMethodInvocation(tree: Term): MethodInvocation =
    tree match {
      case Select(instance @ Ident(_), methodName) =>
        MethodInvocation(instance, methodName, Nil)
      case Apply(Select(instance @ Ident(_), methodName), args) =>
        MethodInvocation(instance, methodName, args)
      case _ => sys.error(s"Expected simple method invocation, got tree of class ${tree.getClass}: $tree")
    }

  case class MethodInvocation(instance: Ident, methodName: String, args: List[Term]) {
    val workflowType = getWorkflowType(instance.tpe.widen)

//    println(workflowType.typeSymbol.methodMembers)

    def getMethod(errorDetails: => String): MethodInfo =
//      println(instance.tpe.typeSymbol.methodMembers)
//      println(instance.tpe.widen.baseClasses)
      workflowType.typeSymbol
        .methodMember(methodName)
        .headOption
        .map(MethodInfo(methodName, _, args))
        .getOrElse(
          sys.error(s"${instance.tpe} doesn't have a $methodName method. " + errorDetails)
        )
  }

  case class MethodInfo(name: String, symbol: Symbol, appliedArgs: List[Term]) {

    validateCalls()

    private def validateCalls(): Unit =
      symbol.paramSymss.headOption.foreach { expectedArgs =>
        println(s"expectedArgs=${expectedArgs.map(_.tree)}")
        println(s"appliedArgs=$appliedArgs")
        appliedArgs.zip(expectedArgs).zipWithIndex.foreach { case ((actual, expected), argumentNo) =>
          // TODO: add better error message for ClassCastException
          val expectedType = expected.tree.asInstanceOf[ValDef].tpt.tpe
          if (!(actual.tpe <:< expectedType)) {
            sys.error(
              s"Provided arguments for method $name doesn't confirm to it's signature:\n" +
                s"\tExpected: $expected (argument #${argumentNo + 1})\n" +
                s"\tGot: $actual (of type ${actual.tpe})"
            )
          }
        }
      }
  }

  case class LambdaConversionResult(tree: Term, typeAscription: TypeRepr, typeArgs: List[TypeRepr], args: List[Term])

  def getWorkflowType(workflow: TypeRepr): TypeRepr = {
    def errorNotWorkflow = sys.error(s"${workflow.show} is not a workflow!")

    println(workflow.getClass)
    workflow match {
      case AppliedType(tc, List(wf)) =>
        val hasAnnotation = wf.typeSymbol.hasAnnotation(WorkflowInterface)
        println(s"has=$hasAnnotation")
        if (!hasAnnotation) errorNotWorkflow
        wf
    }
  }

// TODO: implement
  def scalaLambdaToFunction(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        TypeRepr
  ): LambdaConversionResult = {
    val f = invocation.instance.select(method.symbol).asExpr
    method.appliedArgs match {
      case args @ List(first) =>
        val aTpe = first.tpe.widen
        val tree = '{(a: $aTpe) => $f(a)}
        ???
    }
  }
}
