package zio.temporal.activity

import zio.temporal.JavaTypeTag
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted.*
import zio.temporal.workflow.ZAsync

trait ZActivityExecutionSyntax {
  inline def execute[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): R =
    ${ ZActivityExecutionSyntax.executeImpl[R]('f, 'javaTypeTag) }

  inline def executeAsync[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): ZAsync[R] =
    ${ ZActivityExecutionSyntax.executeAsyncImpl[R]('f, 'javaTypeTag) }
}

object ZActivityExecutionSyntax {
  def executeImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[R] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfActivity(f.asTerm)
    assertTypedActivityStub(invocation.tpe, "execute")

    val method       = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val activityName = getActivityName(method.symbol)

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ActivityStub]

    '{
      TemporalWorkflowFacade.executeActivity($stub, ${ Expr(activityName) }, ${ method.argsExpr })($javaTypeTag)
    }.debugged(SharedCompileTimeMessages.generatedActivityExecute)
  }

  def executeAsyncImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[ZAsync[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfActivity(f.asTerm)
    assertTypedActivityStub(invocation.tpe, "executeAsync")

    val method       = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val activityName = getActivityName(method.symbol)

    val stub = invocation.selectJavaReprOf[io.temporal.workflow.ActivityStub]

    '{
      ZAsync.fromJava(
        TemporalWorkflowFacade.executeActivityAsync($stub, ${ Expr(activityName) }, ${ method.argsExpr })(
          $javaTypeTag
        )
      )
    }.debugged(SharedCompileTimeMessages.generatedActivityExecuteAsync)
  }
}
