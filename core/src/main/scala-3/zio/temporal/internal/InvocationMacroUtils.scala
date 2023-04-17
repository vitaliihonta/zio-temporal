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

  // TODO: add same for activity
  private val zworkflowStub         = TypeRepr.of[ZWorkflowStub]
  private val zchildWorkflowStub    = TypeRepr.of[ZChildWorkflowStub]
  private val zexternalWorkflowStub = TypeRepr.of[ZExternalWorkflowStub]
  private val zactivityStub         = TypeRepr.of[ZActivityStub]

  def betaReduceExpression[A: Type](f: Expr[A]): Expr[A] =
    Expr.betaReduce(f).asTerm.underlying.asExprOf[A]

  def getMethodInvocationOfWorkflow(tree: Term): MethodInvocation =
    getMethodInvocation(tree, getWorkflowType)

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
    // Asserts that this is a WorkflowInterface
    private val tpe = disassembleType(instance.tpe.widen)

    def getMethod(errorDetails: => String): MethodInfo =
      tpe.typeSymbol
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

  def getWorkflowType(workflow: TypeRepr): TypeRepr =
    findWorkflowType(workflow).getOrElse(error(SharedCompileTimeMessages.notWorkflow(workflow.show)))

  def getActivityType(workflow: TypeRepr): TypeRepr =
    findActivityType(workflow).getOrElse(error(SharedCompileTimeMessages.notActivity(workflow.show)))

  def findWorkflowType(workflow: TypeRepr): Option[TypeRepr] = {
    val tpe = workflow match {
      case AndType(left, wf)
          if left =:= zworkflowStub ||
            left =:= zchildWorkflowStub ||
            left =:= zexternalWorkflowStub =>
        wf

      case AppliedType(_, List(wf)) => wf
      case _                        => workflow
    }
    if (!isWorkflow(tpe.typeSymbol)) None
    else Some(tpe)
  }

  def findActivityType(activity: TypeRepr): Option[TypeRepr] = {
    val tpe = activity match {
      case AndType(left, act) if left =:= zactivityStub =>
        act
      case AppliedType(_, List(act)) => act
      case _                         => activity
    }
    if (!isActivity(tpe.typeSymbol)) None
    else Some(tpe)
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

  // Workflow#start
  def buildStartWorkflowInvocation(f: Term): Expr[WorkflowExecution] = {
    val invocation = getMethodInvocationOfWorkflow(f)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    workflowStartInvocation(invocation, method)
  }

  def workflowStartInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo
  ): Expr[WorkflowExecution] = {
    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.client.WorkflowStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{ TemporalWorkflowFacade.start($stub, $castedArgs) }
  }

  // Workflow#execute
  def buildExecuteWorkflowInvocation[R: Type](f: Term, ctgExpr: Expr[ClassTag[R]]): Expr[CompletableFuture[R]] = {
    val invocation = getMethodInvocationOfWorkflow(f)

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()

    workflowExecuteInvocation(invocation, method, ctgExpr)
  }

  def workflowExecuteInvocation[R: Type](
    invocation: MethodInvocation,
    method:     MethodInfo,
    ctgExpr:    Expr[ClassTag[R]]
  ): Expr[CompletableFuture[R]] = {
    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.client.WorkflowStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{ TemporalWorkflowFacade.execute($stub, $castedArgs)($ctgExpr) }
  }

  // Workflow#query
  def buildQueryInvocation[R: Type](f: Term, ctgExpr: Expr[ClassTag[R]]): Expr[R] = {
    val invocation = getMethodInvocationOfWorkflow(f)

    val method = invocation.getMethod(SharedCompileTimeMessages.qrMethodShouldntBeExtMethod)
    method.assertQueryMethod()
    val queryName = getQueryName(method.symbol)

    queryInvocation(invocation, method, queryName, ctgExpr)
  }

  def queryInvocation[R: Type](
    invocation: MethodInvocation,
    method:     MethodInfo,
    queryName:  String,
    ctgExpr:    Expr[ClassTag[R]]
  ): Expr[R] = {
    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.client.WorkflowStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{ TemporalWorkflowFacade.query[R]($stub, ${ Expr(queryName) }, $castedArgs)($ctgExpr) }
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
