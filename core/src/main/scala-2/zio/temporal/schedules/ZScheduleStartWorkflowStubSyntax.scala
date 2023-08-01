package zio.temporal.schedules

import zio.temporal.internal.ZScheduleStartWorkflowMacro
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZScheduleStartWorkflowStubSyntax {
  // todo: document
  def start[A](f: A): ZScheduleAction.StartWorkflow =
    macro ZScheduleStartWorkflowMacro.startImpl[A]
}
