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
    validateNoDefaultArgs()

    def assertWorkflowMethod(): Unit =
      if (!hasAnnotation(symbol, WorkflowMethod)) {
        error(SharedCompileTimeMessages.notWorkflowMethod(symbol.toString))
      }

    def assertSignalMethod(): Unit =
      if (!hasAnnotation(symbol, SignalMethod)) {
        error(SharedCompileTimeMessages.notSignalMethod(symbol.toString))
      }

    def assertQueryMethod(): Unit =
      if (!hasAnnotation(symbol, QueryMethod)) {
        error(SharedCompileTimeMessages.notQueryMethod(symbol.toString))
      }

    private def validateCalls(): Unit =
      symbol.typeSignature.paramLists.headOption.foreach { expectedArgs =>
        appliedArgs.zip(expectedArgs).zipWithIndex.foreach { case ((actual, expected), argumentNo) =>
          if (!(actual.tpe <:< expected.typeSignature)) {
            error(
              SharedCompileTimeMessages.methodArgumentsMismatch(
                name = name.toString,
                expected = expected.toString,
                argumentNo = argumentNo,
                actual = actual.toString,
                actualTpe = actual.tpe.toString
              )
            )
          }
        }
      }

    private def validateNoDefaultArgs(): Unit = {
      val paramsWithDefault = symbol.asMethod.paramLists
        .flatMap(
          _.map(_.asTerm)
            .filter(_.isParamWithDefault)
        )
      if (paramsWithDefault.nonEmpty) {
        error(
          SharedCompileTimeMessages.defaultArgumentsNotSupported(
            paramsWithDefault.map(_.name.toString)
          )
        )
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
          error(
            SharedCompileTimeMessages.methodNotFound(
              instanceTpe = instance.tpe.toString,
              methodName = methodName.toString,
              errorDetails = errorDetails
            )
          )
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
      case _ =>
        error(
          SharedCompileTimeMessages.expectedSimpleMethodInvocation(
            tree.getClass,
            tree.toString
          )
        )
    }

  protected def getTag[A: WeakTypeTag] = {
    val tagTpe = weakTypeOf[Tag[A]]
    findImplicit(tagTpe, SharedCompileTimeMessages.notFound(tagTpe.toString))
  }

  protected def assertWorkflow(workflow: Type): Type = {
    def errorNotWorkflow = error(SharedCompileTimeMessages.notWorkflow(workflow.toString))
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
