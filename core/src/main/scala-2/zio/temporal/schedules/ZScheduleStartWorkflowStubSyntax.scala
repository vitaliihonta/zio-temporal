package zio.temporal.schedules

import zio.temporal.internal.ZScheduleStartWorkflowMacro
import scala.language.experimental.macros
import scala.language.implicitConversions

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
  def start[A](f: A): ZScheduleAction.StartWorkflow =
    macro ZScheduleStartWorkflowMacro.startImpl[A]
}
