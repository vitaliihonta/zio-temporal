package zio.temporal.internal

import zio.temporal.*
import scala.quoted.*
import scala.reflect.ClassTag

class InvocationMacroUtils[Q <: Quotes](using override val q: Q) extends MacroUtils[Q] {
  import q.reflect.*

  private val ActivityInterface = typeSymbolOf[activityInterface]
  private val WorkflowInterface = typeSymbolOf[workflowInterface]
  private val WorkflowMethod    = typeSymbolOf[workflowMethod]
  private val QueryMethod       = typeSymbolOf[queryMethod]
  private val SignalMethod      = typeSymbolOf[signalMethod]

  def betaReduceExpression[A: Type](f: Expr[A]): Expr[A] =
    Expr.betaReduce(f).asTerm.underlying.asExprOf[A]

  def getMethodInvocation(tree: Term): MethodInvocation =
    tree match {
      case Select(instance, methodName) =>
        MethodInvocation(instance, methodName, Nil)
      case Apply(Select(instance, methodName), args) =>
        MethodInvocation(instance, methodName, args)
      case TypeApply(inner, _) =>
        getMethodInvocation(inner)
      case Block(List(inner: Term), _) =>
        getMethodInvocation(inner)
      case _ => sys.error(s"Expected simple method invocation, got tree of class ${tree.getClass}: $tree")
    }

  case class MethodInvocation(instance: Term, methodName: String, args: List[Term]) {
    // Asserts that this is a WorkflowInterface
    val workflowType = getWorkflowType(instance.tpe.widen)

    def getMethod(errorDetails: => String): MethodInfo =
      workflowType.typeSymbol
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

  def findWorkflowType(workflow: TypeRepr): Option[TypeRepr] = {
    val tpe = workflow match {
      case AppliedType(_, List(wf)) => wf
      case _                        => workflow
    }
    if (!isWorkflow(tpe.typeSymbol)) None
    else Some(tpe)
  }

  def findActivityType(activity: TypeRepr): Option[TypeRepr] = {
    val tpe = activity match {
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

  def buildQueryInvocation[R: Type](f: Term, ctgExpr: Expr[ClassTag[R]]): Expr[R] = {
    val invocation = getMethodInvocation(f)

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
      method.appliedArgs.map(_.asExprOf[AnyRef])
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
