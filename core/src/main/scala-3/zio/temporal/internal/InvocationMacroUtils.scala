package zio.temporal.internal

import io.temporal.api.common.v1.WorkflowExecution
import zio.temporal.*
import zio.temporal.activity.ZActivityStub
import zio.temporal.workflow.*

import java.util.concurrent.CompletableFuture
import scala.quoted.*
import scala.reflect.ClassTag

class InvocationMacroUtils[Q <: Quotes](using override val q: Q) extends MacroUtils[Q] {
  import q.reflect.*

  private val ActivityInterface = typeSymbolOf[activityInterface]
  private val WorkflowInterface = typeSymbolOf[workflowInterface]
  private val WorkflowMethod    = typeSymbolOf[workflowMethod]
  private val QueryMethod       = typeSymbolOf[queryMethod]
  private val SignalMethod      = typeSymbolOf[signalMethod]
  private val ActivityMethod    = typeSymbolOf[activityMethod]

  private val zworkflowStub              = TypeRepr.of[ZWorkflowStub]
  private val zchildWorkflowStub         = TypeRepr.of[ZChildWorkflowStub]
  private val zexternalWorkflowStub      = TypeRepr.of[ZExternalWorkflowStub]
  private val zworkflowContinueAsNewStub = TypeRepr.of[ZWorkflowContinueAsNewStub]
  private val zactivityStub              = TypeRepr.of[ZActivityStub]

  def betaReduceExpression[A: Type](f: Expr[A]): Expr[A] =
    Expr.betaReduce(f).asTerm.underlying.asExprOf[A]

  // Asserts that this is a WorkflowInterface
  def getMethodInvocationOfWorkflow(tree: Term): MethodInvocation =
    getMethodInvocation(tree, getWorkflowInterface)

  // Asserts that this is a ActivityInterface
  def getMethodInvocationOfActivity(tree: Term): MethodInvocation =
    getMethodInvocation(tree, getActivityType)

  private def getMethodInvocation(tree: Term, disassembleType: TypeRepr => TypeRepr): MethodInvocation =
    tree match {
      case Inlined(_, _, body) =>
        getMethodInvocation(body, disassembleType)
      case Select(instance, methodName) =>
        MethodInvocation(instance, methodName, Nil, disassembleType)
      case Apply(Select(instance, methodName), args) =>
        MethodInvocation(instance, methodName, args, disassembleType)
      case TypeApply(inner, _) =>
        getMethodInvocation(inner, disassembleType)
      case Block(List(inner: Term), _) =>
        getMethodInvocation(inner, disassembleType)
      case _ => sys.error(s"Expected simple method invocation, got tree of class ${tree.getClass}: $tree")
    }

  case class MethodInvocation(
    instance:        Term,
    methodName:      String,
    args:            List[Term],
    disassembleType: TypeRepr => TypeRepr) {

    val tpe: TypeRepr         = instance.tpe.widen
    private val unwrappedType = disassembleType(instance.tpe.widen)

    def selectJavaReprOf[T: Type]: Expr[T] =
      instance
        .select(instance.symbol.methodMember("toJava").head)
        .asExprOf[T]

    def getMethod(errorDetails: => String): MethodInfo =
      unwrappedType.typeSymbol
        .methodMember(methodName)
        .headOption
        .map(MethodInfo(methodName, _, args))
        .getOrElse(
          error(
            SharedCompileTimeMessages.methodNotFound(
              instance.tpe.toString,
              methodName,
              errorDetails
            )
          )
        )
  }

  case class MethodInfo(name: String, symbol: Symbol, appliedArgs: List[Term]) {

    validateCalls()
    validateNoDefaultArgs()

    def assertWorkflowMethod(): Unit =
      if (!symbol.hasAnnotation(WorkflowMethod)) {
        error(SharedCompileTimeMessages.notWorkflowMethod(symbol.toString))
      }

    def assertSignalMethod(): Unit =
      if (!symbol.hasAnnotation(SignalMethod)) {
        error(SharedCompileTimeMessages.notSignalMethod(symbol.toString))
      }

    def assertQueryMethod(): Unit =
      if (!symbol.hasAnnotation(QueryMethod)) {
        error(SharedCompileTimeMessages.notQueryMethod(symbol.toString))
      }

    def argsExpr: Expr[List[Any]] = Expr.ofList(
      appliedArgs.map(_.asExprOf[Any])
    )

    private def validateCalls(): Unit =
      symbol.paramSymss.headOption.foreach { expectedArgs =>
        appliedArgs.zip(expectedArgs).zipWithIndex.foreach { case ((actual, expected), argumentNo) =>
          expected.tree match {
            case vd: ValDef =>
              val expectedType = vd.tpt.tpe
              if (!(actual.tpe <:< expectedType)) {
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
            case other =>
              error(
                SharedCompileTimeMessages.unexpectedLibraryError(
                  s"error while validating method invocation arguments: " +
                    s"unexpected tree in `expected arguments` symbol:\n" +
                    s"class: ${other.getClass}\n" +
                    s"tree: $other"
                )
              )
          }
        }
      }

    private def validateNoDefaultArgs(): Unit = {
      val paramsWithDefault = symbol.paramSymss
        .flatMap(
          _.filter(_.flags is Flags.HasDefault)
        )
      if (paramsWithDefault.nonEmpty) {
        error(
          SharedCompileTimeMessages.defaultArgumentsNotSupported(
            paramsWithDefault.map(_.name)
          )
        )
      }
    }
  }

  def getWorkflowInterface(workflow: TypeRepr): TypeRepr =
    findWorkflowType(workflow).getOrElse(error(SharedCompileTimeMessages.notWorkflow(workflow.show)))

  def getActivityType(workflow: TypeRepr): TypeRepr =
    findActivityType(workflow).getOrElse(error(SharedCompileTimeMessages.notActivity(workflow.show)))

  def findWorkflowType(workflow: TypeRepr): Option[TypeRepr] = {
    val tpe = workflow.dealias match {
      case AndType(left, wf) => wf
      case other             => other
    }
    if (!isWorkflow(tpe.typeSymbol)) None
    else Some(tpe)
  }

  def assertTypedWorkflowStub(workflow: TypeRepr, stubType: String, method: String): TypeRepr = {
    workflow.dealias match {
      case AndType(left, wf)
          if left =:= zworkflowStub ||
            left =:= zchildWorkflowStub ||
            left =:= zexternalWorkflowStub ||
            left =:= zworkflowContinueAsNewStub =>
        wf
      case other =>
        error(
          SharedCompileTimeMessages.usingNonStubOf(stubType, method, other.show)
        )
    }
  }

  def findActivityType(activity: TypeRepr): Option[TypeRepr] = {
    val tpe = activity.dealias match {
      case AndType(left, act) => act
      case other              => other
    }
    if (!isActivity(tpe.typeSymbol)) None
    else Some(tpe)
  }

  def assertTypedActivityStub(activity: TypeRepr, method: String): TypeRepr = {
    activity.dealias match {
      case AndType(left, act) if left =:= zactivityStub =>
        act
      case other =>
        error(
          SharedCompileTimeMessages.usingNonStubOf("ZActivityStub", method, other.show)
        )
    }
  }

  def getWorkflowType(workflow: TypeRepr): String = {
    val interface = getWorkflowInterface(workflow)
    interface.typeSymbol.declaredMethods
      .map(findWorkflowTypeInMethod)
      .find(_.isDefined)
      .flatten
      .getOrElse(
        interface.typeSymbol.name.toString
      )
  }

  private def findWorkflowTypeInMethod(method: Symbol): Option[String] =
    method.getAnnotation(WorkflowMethod) match {
      case Some(Apply(Select(New(_), _), List(NamedArg(_, Literal(StringConstant(name)))))) =>
        Some(name)
      case _ => None
    }

  def isWorkflow(sym: Symbol): Boolean =
    sym.hasAnnotation(WorkflowInterface)

  def extendsWorkflow(tpe: TypeRepr): Boolean =
    findWorkflowType(tpe).isDefined || tpe.baseClasses.exists(isWorkflow)

  def assertExtendsWorkflow(workflow: TypeRepr): Unit =
    if (!extendsWorkflow(workflow)) {
      error(SharedCompileTimeMessages.notWorkflow(workflow.show))
    }

  def isActivity(sym: Symbol): Boolean =
    sym.hasAnnotation(ActivityInterface)

  def extendsActivity(tpe: TypeRepr): Boolean =
    findActivityType(tpe).isDefined || tpe.baseClasses.exists(isActivity)

  def assertExtendsActivity(activity: TypeRepr): Unit =
    if (!extendsActivity(activity)) {
      error(SharedCompileTimeMessages.notActivity(activity.show))
    }

  def getQueryName(method: Symbol): String = {
    val ann = method.getAnnotation(QueryMethod)
    ann match {
      case Some(Apply(Select(New(_), _), List(NamedArg(_, Literal(StringConstant(name)))))) =>
        name
      case _ => method.name
    }
  }

  def getActivityName(method: Symbol): String = {
    def methodNameCap = method.name.capitalize
    if (!method.hasAnnotation(ActivityMethod)) {
      methodNameCap
    } else {
      method.getAnnotation(ActivityMethod) match {
        case Some(Apply(Select(New(_), _), List(NamedArg(_, Literal(StringConstant(name)))))) =>
          name
        case _ => methodNameCap
      }
    }
  }

  def getSignalName(method: Symbol): String = {
    val ann = method.getAnnotation(SignalMethod)
    ann match {
      case Some(Apply(Select(New(_), _), List(NamedArg(_, Literal(StringConstant(name)))))) =>
        name
      case _ => method.name
    }
  }

  private def typeSymbolOf[A: Type]: Symbol =
    TypeRepr.of[A].dealias.typeSymbol
}
