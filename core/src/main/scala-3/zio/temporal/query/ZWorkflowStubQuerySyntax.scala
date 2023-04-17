package zio.temporal.query

import zio.temporal.TemporalIO
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted.*
import scala.reflect.ClassTag

trait ZWorkflowStubQuerySyntax {
  inline def query[R](inline f: R)(using ctg: ClassTag[R]): TemporalIO[R] =
    ${ ZWorkflowStubQuerySyntax.queryImpl[R]('f, 'ctg) }
}

object ZWorkflowStubQuerySyntax {
  def queryImpl[R: Type](
    f:       Expr[R],
    ctg:     Expr[ClassTag[R]]
  )(using q: Quotes
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
        TemporalWorkflowFacade.query[R]($stub, ${ Expr(queryName) }, ${ method.argsExpr })($ctg)
      }
    }.debugged(SharedCompileTimeMessages.generatedQueryInvoke)
  }
}
