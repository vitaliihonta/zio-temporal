package zio.temporal.testkit

import io.temporal.testing.WorkflowReplayer
import zio._
import zio.temporal.ZWorkflowExecutionHistory
import zio.temporal.worker.ZWorker
import zio.temporal.workflow.ExtendsWorkflow

// TODO: finish it
/** Replays a workflow given its history. Useful for backwards compatibility testing. */
final class ZWorkflowReplayer private (testWorkflowEnvironment: ZTestWorkflowEnvironment[Any]) {

  /** Replays workflow from a [[ZWorkflowExecutionHistory]].
    *
    * @param history
    *   object that contains the workflow ids and the events.
    * @param workflowClass
    *   s workflow implementation class to replay
    * @param moreWorkflowClasses
    *   optional additional workflow implementation classes (like child or external workflows)
    * @throws Exception
    *   if replay failed for any reason.
    */
  def replayWorkflowExecution(
    history:             ZWorkflowExecutionHistory,
    workflowClass:       Class[_],
    moreWorkflowClasses: Class[_]*
  ): Task[Unit] = ZIO.attemptBlocking {
    WorkflowReplayer.replayWorkflowExecution(
      history.toJava,
      workflowClass,
      moreWorkflowClasses: _*
    )
  }
}

object ZWorkflowReplayer {
  val make: URLayer[ZTestWorkflowEnvironment[Any], ZWorkflowReplayer] =
    ZLayer.fromFunction(new ZWorkflowReplayer(_: ZTestWorkflowEnvironment[Any]))
}
