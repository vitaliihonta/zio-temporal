package zio.temporal.internal

import zio.temporal.queryMethod
import zio.temporal.workflow.ZWorkflowStub

import scala.reflect.macros.blackbox

// TODO: Simplify it as in Scala 3
class ZWorkflowQueryMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val QueryMethod = typeOf[queryMethod].dealias

  def newQueryImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val theQuery = buildQueryInvocation(f.tree, weakTypeOf[R])

    q"""
       _root_.zio.temporal.internal.TemporalInteraction.from {
         $theQuery
       }
     """.debugged("Generated query invocation")
  }

  def newQueryEitherImpl[E: WeakTypeTag, R: WeakTypeTag](f: Expr[Either[E, R]]): Tree = {
    val theQuery = buildQueryInvocation(f.tree, weakTypeOf[Either[E, R]])

    q"""
       _root_.zio.temporal.internal.TemporalInteraction.fromEither {
         $theQuery
       }
     """.debugged("Generated query invocation")
  }

  private def buildQueryInvocation(f: Tree, ret: Type): Tree = {
    val invocation = getMethodInvocation(f)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod("Query method should not be an extension method!")

    val queryName = getQueryName(method.symbol)

    queryInvocation(invocation, method, queryName, ret)
  }

  private def getQueryName(method: Symbol): String =
    getAnnotation(method, QueryMethod).children.tail
      .collectFirst { case NamedArgVersionSpecific(_, Literal(Constant(queryName: String))) =>
        queryName
      }
      .getOrElse(method.name.toString)

  private def queryInvocation(
    invocation: MethodInvocation,
    method:     MethodInfo,
    queryName:  String,
    ret:        Type
  ): Tree = {
    val stub = q"""${invocation.instance}.toJava"""
    method.appliedArgs match {
      case Nil =>
        q"""$stub.query[$ret]($queryName, classOf[$ret])"""
      case List(a) =>
        q"""$stub.query[$ret]($queryName, classOf[$ret], $a.asInstanceOf[AnyRef])"""
      case List(a, b) =>
        q"""$stub.query[$ret]($queryName, classOf[$ret], $a.asInstanceOf[AnyRef], $b.asInstanceOf[AnyRef])"""
      case args =>
        sys.error(s"Query with arity ${args.size} not currently implemented. Feel free to contribute!")
    }
  }
}
