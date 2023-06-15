package zio.temporal.activity

import zio.temporal.JavaTypeTag
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted._
import zio.temporal.workflow.ZAsync

trait ZActivityExecutionSyntax {

  /** Executes the given activity synchronously. Accepts the activity method invocation
    *
    * Example:
    * {{{
    *   val stub: ZActivityStub.Of[T] = ???
    *
    *   val result: R = ZActivityStub.execute(
    *     stub.someMethod(someArg)
    *   )
    * }}}
    *
    * @tparam R
    *   activity result type
    * @param f
    *   the activity invocation
    * @return
    *   the activity result
    */
  inline def execute[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): R =
    ${ ZActivityExecutionSyntax.executeImpl[R]('f, 'javaTypeTag) }

  /** Executes the given activity asynchronously. Accepts the activity method invocation
    *
    * Example:
    * {{{
    *   val stub: ZActivityStub.Of[T] = ???
    *
    *   val result: ZAsync[R] = ZActivityStub.executeAsync(
    *     stub.someMethod(someArg)
    *   )
    * }}}
    *
    * @tparam R
    *   activity result type
    * @param f
    *   the activity invocation
    * @return
    *   the activity result (async)
    */
  inline def executeAsync[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): ZAsync[R] =
    ${ ZActivityExecutionSyntax.executeAsyncImpl[R]('f, 'javaTypeTag) }
}

object ZActivityExecutionSyntax {
  def executeImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[R] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedActivityStub(invocation.tpe, "execute")

    val method = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    method.warnPossibleSerializationIssues()

    val methodName = method.symbol.name

    val stub         = invocation.selectJavaReprOf[io.temporal.workflow.ActivityStub]
    val stubbedClass = invocation.selectStubbedClass

    '{
      TemporalWorkflowFacade.executeActivity(
        $stub,
        $stubbedClass,
        ${ Expr(methodName) },
        ${ method.argsExpr }
      )($javaTypeTag)
    }.debugged(SharedCompileTimeMessages.generatedActivityExecute)
  }

  def executeAsyncImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[ZAsync[R]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedActivityStub(invocation.tpe, "executeAsync")

    val method = invocation.getMethod(SharedCompileTimeMessages.actMethodShouldntBeExtMethod)
    method.warnPossibleSerializationIssues()

    val methodName = method.symbol.name

    val stub         = invocation.selectJavaReprOf[io.temporal.workflow.ActivityStub]
    val stubbedClass = invocation.selectStubbedClass

    '{
      ZAsync.fromJava(
        TemporalWorkflowFacade.executeActivityAsync(
          $stub,
          $stubbedClass,
          ${ Expr(methodName) },
          ${ method.argsExpr }
        )($javaTypeTag)
      )
    }.debugged(SharedCompileTimeMessages.generatedActivityExecuteAsync)
  }
}
