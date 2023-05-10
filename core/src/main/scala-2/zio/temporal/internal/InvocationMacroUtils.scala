package zio.temporal.internal

import izumi.reflect.Tag
import zio.temporal.*
import zio.temporal.activity.{IsActivity, ZActivityStub}
import zio.temporal.workflow.IsWorkflow
import scala.reflect.macros.blackbox

abstract class InvocationMacroUtils(override val c: blackbox.Context)
    extends MacroUtils(c)
    with VersionSpecificMacroUtils {

  import c.universe._

  protected val ActivityInterface = typeOf[activityInterface].dealias
  protected val WorkflowInterface = typeOf[workflowInterface].dealias
  protected val WorkflowMethod    = typeOf[workflowMethod].dealias
  protected val QueryMethod       = typeOf[queryMethod].dealias
  protected val SignalMethod      = typeOf[signalMethod].dealias
  protected val ActivityMethod    = typeOf[activityMethod].dealias

  protected val ZActivityStubType = typeOf[ZActivityStub].dealias

  protected val IsWorkflowImplicitTC = typeOf[IsWorkflow[Any]].typeConstructor
  protected val IsActivityImplicitC  = typeOf[IsActivity[Any]].typeConstructor

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

  /** @note
    *   Type method annotations are missing during Scala 2 macro expansion in case we have only a WeakTypeTag.
    */
  protected def getWorkflowType(workflow: Type): String = {
    val interface = getWorkflowInterface(workflow)
    interface.decls
      .filter(_.isMethod)
      .map(findWorkflowTypeInMethod)
      .find(_.isDefined)
      .flatten
      .getOrElse(
        interface.typeSymbol.name.toString
      )
  }

  private def findWorkflowTypeInMethod(method: Symbol): Option[String] = {
    findAnnotation(method, WorkflowMethod)
      .flatMap { t =>
        t.children.tail
          .collectFirst { case NamedArgVersionSpecific(_, Literal(Constant(workflowType: String))) =>
            workflowType
          }
      }
  }

  protected def getSignalName(method: Symbol): String =
    getAnnotation(method, SignalMethod).children.tail
      .collectFirst { case NamedArgVersionSpecific(_, Literal(Constant(signalName: String))) =>
        signalName
      }
      .getOrElse(method.name.toString)

  protected def assertWorkflow(workflow: Type, isFromImplicit: Boolean): Type = {
    if (isWorkflow(workflow) || isWorkflowImplicitProvided(workflow, isFromImplicit)) {
      workflow
    } else {
      error(SharedCompileTimeMessages.notWorkflow(workflow.toString))
    }
  }

  private def isWorkflowImplicitProvided(workflow: Type, isFromImplicit: Boolean): Boolean = {
    // Don't infer implicit IsWorkflow in case that the IsWorkflow derivation
    if (isFromImplicit) false
    else {
      val searchRes = {
        c.typecheck(
          c.inferImplicitValue(appliedType(IsWorkflowImplicitTC, workflow), silent = true),
          silent = true
        )
      }
      searchRes != EmptyTree
    }
  }

  protected def assertTypedWorkflowStub(workflow: Type, stubType: Type, method: String): Type = {
    workflow.dealias match {
      case SingleType(_, sym) =>
        assertTypedWorkflowStub(sym.typeSignature.finalResultType.dealias, stubType, method)
      case RefinedType(List(stub, wf), _) =>
        // NOTE: used assertWorkflow before, but it's too restrictive.
        // Checking the stubType instead allows usage of polymorphic workflow interfaces.
        // The fact that the stub was built guarantees that the workflow/signal/query method was invoked on a valid stub
        if (!(stub =:= stubType))
          error(SharedCompileTimeMessages.usingNonStubOf(stubType.toString, method, workflow.toString))
        else workflow
      case other =>
        println(other.getClass)
        error(SharedCompileTimeMessages.usingNonStubOf(stubType.toString, method, other.toString))
    }
  }

  protected def assertActivity(activity: Type, isFromImplicit: Boolean): Type = {
    if (isActivity(activity) || isActivityImplicitProvided(activity, isFromImplicit)) {
      activity
    } else {
      error(SharedCompileTimeMessages.notActivity(activity.toString))
    }
  }

  private def isActivityImplicitProvided(workflow: Type, isFromImplicit: Boolean): Boolean = {
    // Don't infer implicit IsActivity in case that the IsActivity derivation
    if (isFromImplicit) false
    else {
      val searchRes = c.typecheck(
        c.inferImplicitValue(appliedType(IsActivityImplicitC, workflow), silent = true),
        silent = true
      )
      searchRes != EmptyTree
    }
  }

  protected def assertTypedActivityStub(activity: Type, method: String): Type = {
    activity.dealias match {
      case SingleType(_, sym) =>
        sym.typeSignature.finalResultType.dealias match {
          case RefinedType(List(stub, act), _) =>
            // NOTE: used assertActivity before, but it's too restrictive.
            // Checking the stubType instead allows usage of polymorphic workflow interfaces.
            // The fact that the stub was built guarantees that the activity method was invoked on a valid stub
            if (!(stub =:= ZActivityStubType))
              error(SharedCompileTimeMessages.usingNonStubOf("ZActivityStub", method, activity.toString))
            else act
          case other =>
            error(SharedCompileTimeMessages.usingNonStubOf("ZActivityStub", method, other.toString))
        }
      case other =>
        error(SharedCompileTimeMessages.usingNonStubOf("ZActivityStub", method, other.toString))
    }
  }

  protected def assertExtendsWorkflow(workflow: Type): Type = {
    if (!extendsWorkflow(workflow)) {
      error(SharedCompileTimeMessages.notWorkflow(workflow.toString))
    }
    workflow
  }

  protected def assertExtendsActivity(workflow: Type): Unit =
    if (!extendsActivity(workflow)) {
      error(SharedCompileTimeMessages.notActivity(workflow.toString))
    }

  protected def getWorkflowInterface(workflow: Type): Type =
    findWorkflowInterface(workflow).getOrElse(
      error(SharedCompileTimeMessages.notWorkflow(workflow.toString))
    )

  protected def isWorkflow(tpe: Type): Boolean = {
    hasAnnotation(tpe.typeSymbol, WorkflowInterface)
  }

  protected def findWorkflowInterface(workflow: Type): Option[Type] =
    workflow match {
      case SingleType(_, sym) =>
        sym.typeSignature.baseClasses
          .flatMap(sym => findAnnotation(sym, WorkflowInterface).map(_ => sym.asType.toType))
          .headOption

      case _ =>
        Some(workflow).filter(isWorkflow)
    }

  protected def extendsWorkflow(tpe: Type): Boolean =
    isWorkflow(tpe) || tpe.baseClasses.exists(sym => isWorkflow(sym.typeSignature))

  protected def isActivity(tpe: Type): Boolean =
    tpe match {
      case SingleType(_, sym) =>
        sym.typeSignature.baseClasses
          .flatMap(sym => findAnnotation(sym, ActivityInterface).map(_ => sym.typeSignature))
          .headOption
          .nonEmpty

      case _ =>
        hasAnnotation(tpe.typeSymbol, ActivityInterface)
    }

  protected def extendsActivity(tpe: Type): Boolean =
    isActivity(tpe) || tpe.baseClasses.exists(sym => isActivity(sym.typeSignature))
}
