package zio.temporal.internal

import zio.temporal.queryMethod
import zio.temporal.workflow.ZWorkflowStub

import scala.reflect.macros.blackbox

class ZWorkflowQueryMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  def newQueryImpl[R: WeakTypeTag](f: Expr[R]): Tree = {
    val theQuery = buildQueryInvocation(f.tree, weakTypeOf[R])

    q"""
       _root_.zio.temporal.internal.TemporalInteraction.from {
         $theQuery
       }
     """.debugged(SharedCompileTimeMessages.generatedQueryInvoke)
  }

  def newQueryEitherImpl[E: WeakTypeTag, R: WeakTypeTag](f: Expr[Either[E, R]]): Tree = {
    val theQuery = buildQueryInvocation(f.tree, weakTypeOf[Either[E, R]])

    q"""
       _root_.zio.temporal.internal.TemporalInteraction.fromEither {
         $theQuery
       }
     """.debugged(SharedCompileTimeMessages.generatedQueryInvoke)
  }

  private def buildQueryInvocation(f: Tree, ret: Type): Tree = {
    val invocation = getMethodInvocation(f)

    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod(SharedCompileTimeMessages.qrMethodShouldntBeExtMethod)
    method.assertQueryMethod()

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
    q"""_root_.zio.temporal.internal.TemporalWorkflowFacade.query[$ret]($stub, $queryName, List(..${method.appliedArgs}))"""
  }
}
