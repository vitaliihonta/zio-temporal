package zio.temporal.schedules

import io.temporal.client.WorkflowOptions
import io.temporal.client.schedules.ScheduleActionStartWorkflow
import io.temporal.common.interceptors.Header
import scala.quoted._
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages}

trait ZScheduleStartWorkflowStubSyntax {
  // todo: document
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
