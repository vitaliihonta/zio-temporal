package zio.temporal.query

import zio.temporal.{JavaTypeTag, TemporalIO}
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted.*

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
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfWorkflow(f.asTerm)

    val method = invocation.getMethod(SharedCompileTimeMessages.qrMethodShouldntBeExtMethod)
    method.assertQueryMethod()
    val queryName = getQueryName(method.symbol)

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      _root_.zio.temporal.internal.TemporalInteraction.from[R] {
        TemporalWorkflowFacade.query[R]($stub, ${ Expr(queryName) }, ${ method.argsExpr })($javaTypeTag)
      }
    }.debugged(SharedCompileTimeMessages.generatedQueryInvoke)
  }
}
