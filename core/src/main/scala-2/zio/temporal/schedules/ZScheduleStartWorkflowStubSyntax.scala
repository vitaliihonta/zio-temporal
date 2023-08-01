package zio.temporal.schedules

import zio._
import zio.temporal.internal.ZScheduleStartWorkflowMacro
import zio.temporal._
import scala.language.experimental.macros
import scala.language.implicitConversions

// todo: implement
trait ZScheduleStartWorkflowStubSyntax {
  // todo: document
  def start[A](f: A): ZScheduleAction.StartWorkflow =
    macro ZScheduleStartWorkflowMacro.startImpl[A]
}
