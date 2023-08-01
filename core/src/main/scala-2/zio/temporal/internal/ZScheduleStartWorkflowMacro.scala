package zio.temporal.internal

import scala.reflect.macros.blackbox
import zio.temporal.schedules._

class ZScheduleStartWorkflowMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val zscheduleStartWorkflowStub = typeOf[ZScheduleStartWorkflowStub.type].dealias

  def startImpl[A: WeakTypeTag](f: Expr[A]): Tree = {
    // Assert called on ZScheduleStartWorkflowStub
    assertPrefixType(zscheduleStartWorkflowStub)

    val invocation = getMethodInvocation(f.tree)
    assertTypedWorkflowStub(invocation.instance.tpe, typeOf[ZScheduleStartWorkflowStub], "start")

    val method = invocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    method.assertWorkflowMethod()
    method.warnPossibleSerializationIssues()

    q"""
       new _root_.zio.temporal.schedules.ZScheduleAction.StartWorkflow(
          _root_.io.temporal.client.schedules.ScheduleActionStartWorkflow.newBuilder()
            .setWorkflowType(${invocation.instance}.stubbedClass)
            .setHeader(${invocation.instance}.header)
            .setOptions(${invocation.instance}.workflowOptions)
            .setArguments(..${method.appliedArgs})
            .build()
       )
     """.debugged(SharedCompileTimeMessages.generatedScheduleStartWorkflow)
  }
}
