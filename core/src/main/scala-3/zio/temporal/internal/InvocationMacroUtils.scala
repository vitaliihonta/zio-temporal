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
//        println(s"expectedArgs=${expectedArgs.map(_.tree)}")
//        println(s"appliedArgs=$appliedArgs")
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

  case class LambdaConversionResult(tree: Term, methodOverload: String, typeArgs: List[TypeRepr], args: List[Term])

  def getWorkflowType(workflow: TypeRepr): TypeRepr = {
    def errorNotWorkflow = sys.error(s"${workflow.show} is not a workflow!")

//    println(workflow.getClass)
    workflow match {
      case AppliedType(tc, List(wf)) =>
        val hasAnnotation = wf.typeSymbol.hasAnnotation(WorkflowInterface)
//        println(s"has=$hasAnnotation")
        if (!hasAnnotation) errorNotWorkflow
        wf
    }
  }

  private val workflowClientSymbol = Symbol.classSymbol("io.temporal.client.WorkflowClient").companionModule
  private val workflowStubSymbol   = Symbol.classSymbol("io.temporal.client.WorkflowStub")
  private val workflowStubQueryMethodSymbol = {
    val methods = workflowStubSymbol.methodMember("query")
//    println(methods.map(_.signature.paramSigs))
    methods.find(_.signature.paramSigs.size == 4).head
  }

  // TODO: it's a dirty hack, try to rewrite it
  private val workflowClientModule =
    Expr
      .betaReduce('{ import io.temporal.client.WorkflowClient.QUERY_TYPE_REPLAY_ONLY })
      .asTerm match { case Inlined(_, _, Block(List(Import(t, List(_))), _)) => t }

  def startInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        TypeRepr
  ): Tree = {
    val LambdaConversionResult(tree, methodOverload, typeArgs, args) = scalaLambdaToFunction(invocation, method, ret)
//    println(t)
    val start = workflowClientSymbol
      .methodMember("start")
      .find(_.signature.paramSigs.contains(methodOverload))
      .head
    val startMethod = Select(workflowClientModule, start)
    val targs       = typeArgs.map(targ => TypeTree.of(using targ.asType))
    Apply(TypeApply(startMethod, targs), tree :: args)
  }

  def executeInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        TypeRepr
  ): Tree = {
    val LambdaConversionResult(tree, methodOverload, typeArgs, args) = scalaLambdaToFunction(invocation, method, ret)
    //    println(t)
    val execute = workflowClientSymbol
      .methodMember("execute")
      .find(_.signature.paramSigs.contains(methodOverload))
      .head
    val executeMethod = Select(workflowClientModule, execute)
    val targs         = typeArgs.map(targ => TypeTree.of(using targ.asType))
    Apply(TypeApply(executeMethod, targs), tree :: args)
  }

  def buildExecuteInvocation(f: Term, ret: TypeRepr): Tree = {
    val invocation = getMethodInvocation(f)
    val method     = invocation.getMethod("Workflow method should not be extension methods!")

    // TODO: validate
    //    assertWorkflowMethod(method.symbol)

    executeInvocation(invocation, method, ret)
  }

  def buildQueryInvocation(f: Term, ret: TypeRepr): Tree = {
    val invocation = getMethodInvocation(f)

//    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod("Query method should not be an extension method!")

    val queryName = getQueryName(method.symbol)

    queryInvocation(invocation, method, queryName, ret)
  }

  def queryInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    queryName:  String,
    ret:        TypeRepr
  ): Tree = {
//    println(invocation.instance.symbol.fieldMembers)
    val retTypeTree = TypeTree.of(using ret.asType)
    val stub        = invocation.instance.select(invocation.instance.symbol.methodMember("toJava").head)
//    println(stub.show)
    method.appliedArgs match {
      case Nil =>
//        println(workflowStubQueryMethodSymbol.signature)
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

// TODO: implement more arities
  def scalaLambdaToFunction(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        TypeRepr
  ): LambdaConversionResult = {
    val f = invocation.instance.select(method.symbol)
    method.appliedArgs match {
      case args @ List(first) =>
        val aTpe = first.tpe.widen
        val tpe = MethodType(List("a"))(
          paramInfosExp = _ => List(aTpe),
          resultTypeExp = _ => ret
        )
        val rhsFn = (s: Symbol, trees: List[Tree]) => Apply(f, trees.map(_.asExpr.asTerm))
        val tree  = Lambda(Symbol.spliceOwner, tpe, rhsFn)
        LambdaConversionResult(tree, "io.temporal.workflow.Functions$.Func1", List(aTpe, ret), args)
    }
  }

  def getQueryName(method: Symbol): String = {
    val ann = method.getAnnotation(Symbol.classSymbol("io.temporal.workflow.QueryMethod"))
    ann match {
      case Some(Apply(Select(New(_), _), List(NamedArg(_, Literal(StringConstant(name)))))) =>
        name
      case _ => method.name
    }
  }

  def getSignalName(method: Symbol): String = {
    val ann = method.getAnnotation(Symbol.classSymbol("io.temporal.workflow.SignalMethod"))
    ann match {
      case Some(Apply(Select(New(_), _), List(NamedArg(_, Literal(StringConstant(name)))))) =>
        name
      case _ => method.name
    }
  }
}
