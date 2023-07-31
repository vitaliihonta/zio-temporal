package zio.temporal.schedules

import io.temporal.client.WorkflowOptions
import io.temporal.client.schedules.{ScheduleAction, ScheduleActionStartWorkflow}
import io.temporal.common.converter.EncodedValues
import io.temporal.common.interceptors.Header
import zio.temporal.internalApi

sealed trait ZScheduleAction {
  def toJava: ScheduleAction
}

object ZScheduleAction {

  final class StartWorkflow @internalApi() (
    override val toJava: ScheduleActionStartWorkflow)
      extends ZScheduleAction {

    def workflowType: String = toJava.getWorkflowType

    def options: WorkflowOptions = toJava.getOptions

    def header: Header = toJava.getHeader

    def arguments: EncodedValues = toJava.getArguments

    override def toString: String = {
      s"ZScheduleAction.StartWorkflow(" +
        s"workflowType=$workflowType" +
        s", options=$options" +
        s", header=$header" +
        s", arguments=$arguments" +
        s")"
    }
  }
}
