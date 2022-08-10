package zio.temporal.internal

import zio.temporal.*

import scala.quoted.*
import io.temporal.api.common.v1.WorkflowExecution

import scala.reflect.ClassTag

// TODO replace println and throw with reporting
class InvocationMacroUtils[Q <: Quotes](using val q: Q) {
  import q.reflect.*

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
          sys.error(s"${instance.tpe} doesn't have a $methodName method. " + errorDetails)
        )
  }

  case class MethodInfo(name: String, symbol: Symbol, appliedArgs: List[Term]) {

    validateCalls()
    validateNoDefaultArgs()

    def assertWorkflowMethod(): Unit =
      if (!symbol.hasAnnotation(WorkflowMethod)) {
        sys.error(s"The method is not a @workflowMethod: $symbol")
      }

    def assertSignalMethod(): Unit =
      if (!symbol.hasAnnotation(SignalMethod)) {
        sys.error(s"The method is not a @signalMethod: $symbol")
      }

    def assertQueryMethod(): Unit =
      if (!symbol.hasAnnotation(QueryMethod)) {
        sys.error(s"The method is not a @queryMethod: $symbol")
      }

    private def validateCalls(): Unit =
      symbol.paramSymss.headOption.foreach { expectedArgs =>
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

    private def validateNoDefaultArgs(): Unit = {
      val paramsWithDefault = symbol.paramSymss
        .flatMap(
          _.filter(_.flags is Flags.HasDefault)
        )
      if (paramsWithDefault.nonEmpty) {
        sys.error(
          s"\nCurrently, methods with default arguments are not supported.\n" +
            s"Found the following default arguments: ${paramsWithDefault.map(_.name).mkString(", ")}.\n" +
            s"Temporal doesn't work well with scala's implementation of default arguments, and throws the following error at runtime:\n" +
            s"[Just an example] java.lang.IllegalArgumentException: Missing @WorkflowMethod, @SignalMethod or @QueryMethod annotation on public default scala.Option zio.temporal.fixture.SignalWorkflow.getProgress$$default$$1()"
        )
      }
    }
  }

  def getWorkflowType(workflow: TypeRepr): TypeRepr =
    workflow match {
      case AppliedType(_, List(wf)) =>
        val hasAnnotation = wf.typeSymbol.hasAnnotation(WorkflowInterface)
        if (!hasAnnotation) {
          sys.error(s"${workflow.show} is not a workflow!")
        }
        wf
    }

  private val workflowStubSymbol = Symbol.classSymbol("io.temporal.client.WorkflowStub")
  private val workflowStubQueryMethodSymbol = {
    val methods = workflowStubSymbol.methodMember("query")
    methods.find(_.signature.paramSigs.size == 4).head
  }

  def buildQueryInvocation[R: Type](f: Term, ctgExpr: Expr[ClassTag[R]]): Expr[R] = {
    val invocation = getMethodInvocation(f)

    val method = invocation.getMethod("Query method should not be an extension method!")
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
