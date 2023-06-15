package zio.temporal.workflow

import zio._
import zio.temporal._
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}
import scala.quoted._

trait ZWorkflowExecutionSyntax {

  /** Starts the given workflow. '''Doesn't wait''' for the workflow to finish. Accepts the workflow method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *   val workflowExecution: TemporalIO[ZWorkflowExecution] =
    *      ZWorkflowStub.start(
    *        stub.someMethod(someArg)
    *      )
    * }}}
    *
    * @tparam A
    *   workflow result type
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow execution metadata
    */
  inline def start[A](inline f: A): TemporalIO[ZWorkflowExecution] =
    ${ ZWorkflowExecutionSyntax.startImpl[A]('f) }

  /** Executes the given workflow. '''Waits''' for the workflow to finish. Accepts the workflow method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *   val workflowExecution: TemporalIO[R] =
    *      ZWorkflowStub.execute(
    *        stub.someMethod(someArg)
    *      )
    * }}}
    *
    * @tparam R
    *   workflow result type
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow result
    */
  inline def execute[R](inline f: R)(using javaTypeTag: JavaTypeTag[R]): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeImpl[R]('f, 'javaTypeTag) }

  /** Executes the given workflow with a given timeout. '''Waits''' for the workflow to finish. Accepts the workflow
    * method invocation
    *
    * Example:
    * {{{
    *   val stub: ZWorkflowStub.Of[T] = ???
    *
    *   val workflowExecution: TemporalIO[R] =
    *      ZWorkflowStub.executeWithTimeout(5.seconds)(
    *        stub.someMethod(someArg)
    *      )
    * }}}
    *
    * @tparam R
    *   workflow result type
    * @param timeout
    *   the timeout
    * @param f
    *   the workflow method invocation
    * @return
    *   the workflow result
    */
  inline def executeWithTimeout[R](
    timeout:           Duration
  )(inline f:          R
  )(using javaTypeTag: JavaTypeTag[R]
  ): TemporalIO[R] =
    ${ ZWorkflowExecutionSyntax.executeWithTimeoutImpl[R]('timeout, 'f, 'javaTypeTag) }
}

object ZWorkflowExecutionSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[TemporalIO[ZWorkflowExecution]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowStub], "start")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.from {
        new ZWorkflowExecution(
          TemporalWorkflowFacade.start($stub, ${ method.argsExpr })
        )
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowStart)
  }

  def executeImpl[R: Type](
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[TemporalIO[R]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowStub], "execute")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        TemporalWorkflowFacade.execute($stub, ${ method.argsExpr })($javaTypeTag)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }

  def executeWithTimeoutImpl[R: Type](
    timeout:     Expr[Duration],
    f:           Expr[R],
    javaTypeTag: Expr[JavaTypeTag[R]]
  )(using q:     Quotes
  ): Expr[TemporalIO[R]] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZWorkflowStub], "executeWithTimeout")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    val stub = invocation.selectJavaReprOf[io.temporal.client.WorkflowStub]

    '{
      zio.temporal.internal.TemporalInteraction.fromFuture {
        TemporalWorkflowFacade.executeWithTimeout($stub, $timeout, ${ method.argsExpr })($javaTypeTag)
      }
    }.debugged(SharedCompileTimeMessages.generatedWorkflowExecute)
  }
}
