package zio.temporal.activity

import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted.*
import scala.reflect.ClassTag
import zio.temporal.workflow.ZAsync

trait ZActivityExecutionSyntax {
  inline def execute[R](inline f: R)(using ctg: ClassTag[R]): R =
    ${ ZActivityExecutionSyntax.executeImpl[R]('f, 'ctg) }

  inline def executeAsync[R](inline f: R)(using ctg: ClassTag[R]): ZAsync[R] =
    ${ ZActivityExecutionSyntax.executeAsyncImpl[R]('f, 'ctg) }
}

object ZActivityExecutionSyntax {
  def executeImpl[R: Type](f: Expr[R], ctg: Expr[ClassTag[R]])(using q: Quotes): Expr[R] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfActivity(f.asTerm)

    val method       = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val activityName = getActivityName(method.symbol)

    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.workflow.ActivityStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{
      TemporalWorkflowFacade.executeActivity($stub, ${ Expr(activityName) }, $castedArgs)($ctg)
    }.debugged(SharedCompileTimeMessages.generatedActivityExecute)
  }

  def executeAsyncImpl[R: Type](f: Expr[R], ctg: Expr[ClassTag[R]])(using q: Quotes): Expr[ZAsync[R]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    val invocation = getMethodInvocationOfActivity(f.asTerm)

    val method       = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    val activityName = getActivityName(method.symbol)

    val stub = invocation.instance
      .select(invocation.instance.symbol.methodMember("toJava").head)
      .asExprOf[io.temporal.workflow.ActivityStub]

    val castedArgs = Expr.ofList(
      method.appliedArgs.map(_.asExprOf[Any])
    )

    '{
      ZAsync.fromJava(
        TemporalWorkflowFacade.executeActivityAsync($stub, ${ Expr(activityName) }, $castedArgs)($ctg)
      )
    }.debugged(SharedCompileTimeMessages.generatedActivityExecuteAsync)
  }
}
