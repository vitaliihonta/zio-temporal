package zio.temporal.schedules

import io.temporal.client.WorkflowOptions
import io.temporal.client.schedules.ScheduleActionStartWorkflow
import io.temporal.common.interceptors.Header
import scala.quoted._
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages}

trait ZScheduleStartWorkflowStubSyntax {

  /** Creates a Start Workflow Schedule Action for the given workflow. Accepts the workflow method invocation
    *
    * Example:
    * {{{
    *   val stub: ZScheduleStartWorkflowStub.Of[T] = ???
    *
    *   val action: ZScheduleAction.StartWorkflow =
    *      ZScheduleStartWorkflowStub.start(
    *        stub.someMethod(someArg)
    *      )
    * }}}
    *
    * @tparam A
    *   workflow result type
    * @param f
    *   the workflow method invocation
    * @return
    *   Start Workflow Schedule Action
    */
  inline def start[A](inline f: A): ZScheduleAction.StartWorkflow =
    ${ ZScheduleStartWorkflowStubSyntax.startImpl[A]('f) }
}

object ZScheduleStartWorkflowStubSyntax {
  def startImpl[A: Type](f: Expr[A])(using q: Quotes): Expr[ZScheduleAction.StartWorkflow] = {
    import q.reflect._
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils._

    val invocation = getMethodInvocation(f.asTerm)
    assertTypedWorkflowStub(invocation.tpe, TypeRepr.of[ZScheduleStartWorkflowStub], "start")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    val stubbedClass    = invocation.selectStubbedClass
    val header          = invocation.selectMember[Header]("header")
    val workflowOptions = invocation.selectMember[WorkflowOptions]("workflowOptions")

    '{
      new ZScheduleAction.StartWorkflow(
        ScheduleActionStartWorkflow
          .newBuilder()
          .setWorkflowType(${ stubbedClass })
          .setHeader(${ header })
          .setOptions(${ workflowOptions })
          .setArguments(${ method.argsExpr }: _*)
          .build()
      )
    }.debugged(SharedCompileTimeMessages.generatedScheduleStartWorkflow)
  }
}
