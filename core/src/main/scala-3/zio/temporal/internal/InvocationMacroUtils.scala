package zio.temporal.internal

import zio.temporal.*
import scala.quoted.*
import io.temporal.api.common.v1.WorkflowExecution

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

  def buildQueryInvocation(f: Term, ret: TypeRepr): Tree = {
    val invocation = getMethodInvocation(f)

    val method = invocation.getMethod("Query method should not be an extension method!")
    method.assertQueryMethod()
    val queryName = getQueryName(method.symbol)

    queryInvocation(invocation, method, queryName, ret)
  }

  // TODO: more arity
  def queryInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    queryName:  String,
    ret:        TypeRepr
  ): Tree = {
    val retTypeTree = TypeTree.of(using ret.asType)
    val stub        = invocation.instance.select(invocation.instance.symbol.methodMember("toJava").head)
    method.appliedArgs match {
      case Nil =>
        val t = Apply(
          TypeApply(Select(stub, workflowStubQueryMethodSymbol), List(retTypeTree)),
          List(
            Literal(StringConstant(queryName)),
            Literal(ClassOfConstant(ret)),
            Expr.ofSeq(Nil).asTerm
          )
        )
        t
//        q"""$stub.query[$ret]($queryName, classOf[$ret])"""
//      case List(a) =>
//        q"""$stub.query[$ret]($queryName, classOf[$ret], $a.asInstanceOf[AnyRef])"""
//      case List(a, b) =>
//        q"""$stub.query[$ret]($queryName, classOf[$ret], $a.asInstanceOf[AnyRef], $b.asInstanceOf[AnyRef])"""
//      case args =>
//        sys.error(s"Query with arity ${args.size} not currently implemented. Feel free to contribute!")
    }
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
