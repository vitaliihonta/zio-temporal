package zio.temporal.query

import zio.temporal.{JavaTypeTag, TemporalIO}
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import zio.temporal.workflow.ZWorkflowStub
import scala.quoted._

trait ZWorkflowStubQuerySyntax {
  inline def query[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): TemporalIO[R] =
    ${ ZWorkflowStubQuerySyntax.queryImpl[R]('f, 'javaTypeTag) }
}

object ZWorkflowStubQuerySyntax {
  def queryImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[TemporalIO[R]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowStub], "query")

    val method = invocation.getMethod(SharedCompileTimeMessages.qrMethodShouldntBeExtMethod)
    method.assertQueryMethod()
    method.warnPossibleSerializationIssues()

    val queryName = getQueryName(method.symbol)

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      _root_.zio.temporal.internal.TemporalInteraction.from[R] {
        TemporalWorkflowFacade.query[R]($stub, ${ Expr(queryName) }, ${ method.argsExpr })($javaTypeTag)
      }
    }.debugged(SharedCompileTimeMessages.generatedQueryInvoke)
  }
}
