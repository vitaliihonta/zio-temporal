package zio.temporal.testkit

import io.temporal.testing.WorkflowReplayer
import zio._
import zio.temporal.ZWorkflowExecutionHistory
import zio.temporal.worker.ZWorker
import zio.temporal.workflow.ZWorkflowImplementationClass
import java.nio.file.Path
import scala.jdk.CollectionConverters._

/** Replays a workflow given its history. Useful for backwards compatibility testing. */
object ZWorkflowReplayer {

  /** Replays workflow from a resource that contains a json serialized history.
    *
    * @param resourceName
    *   name of the resource.
    * @param workflowClass
    *   s workflow implementation class to replay
    * @param moreClasses
    *   optional additional workflow implementation classes
    * @throws Exception
    *   if replay failed for any reason.
    */
  def replayWorkflowExecutionFromResource(
    resourceName:  String,
    workflowClass: ZWorkflowImplementationClass[_],
    moreClasses:   ZWorkflowImplementationClass[_]*
  ): RIO[ZTestWorkflowEnvironment[Any], Unit] = {
    for {
      history <- ZWorkflowHistoryLoader.readHistoryFromResource(resourceName)
      _       <- replayWorkflowExecution(history, workflowClass, moreClasses: _*)
    } yield ()
  }

  /** Replays workflow from a resource that contains a json serialized history.
    *
    * @param resourceName
    *   name of the resource.
    * @param worker
    *   worker existing worker with the correct task queue and registered implementations.
    * @throws Exception
    *   if replay failed for any reason.
    */
  def replayWorkflowExecutionFromResource(
    resourceName: String,
    worker:       ZWorker
  ): RIO[ZTestWorkflowEnvironment[Any], Unit] = {
    for {
      history <- ZWorkflowHistoryLoader.readHistoryFromResource(resourceName)
      _       <- replayWorkflowExecution(history, worker)
    } yield ()
  }

  /** Replays workflow from a file
    *
    * @param historyFile
    *   file that contains a json serialized history.
    * @param workflowClass
    *   s workflow implementation class to replay
    * @param moreClasses
    *   optional additional workflow implementation classes
    * @throws Exception
    *   if replay failed for any reason.
    */
  def replayWorkflowExecution(
    historyFile:   Path,
    workflowClass: ZWorkflowImplementationClass[_],
    moreClasses:   ZWorkflowImplementationClass[_]*
  ): RIO[ZTestWorkflowEnvironment[Any], Unit] = {
    for {
      history <- ZWorkflowHistoryLoader.readHistory(historyFile)
      _       <- replayWorkflowExecution(history, workflowClass, moreClasses: _*)
    } yield ()
  }

  /** Replays workflow from a json serialized history. The json should be in the format:
    *
    * <pre> { "workflowId": "...", "runId": "...", "events": [ ... ] } </pre>
    *
    * RunId <b>must</b> match the one used to generate the serialized history.
    *
    * @param jsonSerializedHistory
    *   string that contains the json serialized history.
    * @param workflowClass
    *   s workflow implementation class to replay
    * @param moreClasses
    *   optional additional workflow implementation classes
    * @throws Exception
    *   if replay failed for any reason.
    */
  def replayWorkflowExecution(
    jsonSerializedHistory: String,
    workflowClass:         ZWorkflowImplementationClass[_],
    moreClasses:           ZWorkflowImplementationClass[_]*
  ): RIO[ZTestWorkflowEnvironment[Any], Unit] = {
    for {
      history <- ZWorkflowExecutionHistory.fromJson(jsonSerializedHistory)
      _       <- replayWorkflowExecution(history, workflowClass, moreClasses: _*)
    } yield ()
  }

  /** Replays workflow from a [[ZWorkflowExecutionHistory]].
    *
    * @param history
    *   object that contains the workflow ids and the events.
    * @param workflowClass
    *   s workflow implementation class to replay
    * @param moreClasses
    *   optional additional workflow implementation classes (like child or external workflows)
    * @throws Exception
    *   if replay failed for any reason.
    */
  def replayWorkflowExecution(
    history:       ZWorkflowExecutionHistory,
    workflowClass: ZWorkflowImplementationClass[_],
    moreClasses:   ZWorkflowImplementationClass[_]*
  ): RIO[ZTestWorkflowEnvironment[Any], Unit] = {
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testWorkflowEnvironment =>
      ZIO.attemptBlocking {
        WorkflowReplayer.replayWorkflowExecution(
          history.toJava,
          testWorkflowEnvironment.toJava,
          workflowClass.runtimeClass,
          moreClasses.map(_.runtimeClass): _*
        )
      }
    }
  }

  /** Replays workflow from a [[ZWorkflowExecutionHistory]].
    *
    * @param history
    *   object that contains the workflow ids and the events.
    * @param worker
    *   existing worker with registered workflow implementations.
    * @throws Exception
    *   if replay failed for any reason.
    */
  def replayWorkflowExecution(
    history: ZWorkflowExecutionHistory,
    worker:  ZWorker
  ): Task[Unit] = ZIO.attemptBlocking {
    WorkflowReplayer.replayWorkflowExecution(
      history.toJava,
      worker.toJava
    )
  }

  /** Replays workflows provided by an iterable.
    *
    * @param histories
    *   The histories to be replayed
    * @param failFast
    *   If true, throws upon the first error encountered (if any) during replay. If false, all histories will be
    *   replayed and the returned object contains information about any failures.
    * @return
    *   If `failFast` is false, contains any replay failures encountered.
    * @throws Exception
    *   If replay failed and `failFast` is true.
    */
  def replayWorkflowExecutions(
    histories:     List[ZWorkflowExecutionHistory],
    failFast:      Boolean,
    workflowClass: ZWorkflowImplementationClass[_],
    moreClasses:   ZWorkflowImplementationClass[_]*
  ): RIO[ZTestWorkflowEnvironment[Any], ZReplayResults] = {
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testWorkflowEnvironment =>
      for {
        worker  <- testWorkflowEnvironment.newWorker("replay-task-queue-name")
        _       <- worker.addWorkflowImplementations(workflowClass, moreClasses: _*)
        results <- replayWorkflowExecutions(histories, failFast, worker)
      } yield results
    }
  }

  /** Replays workflows provided by an iterable using an already-initialized worker.
    *
    * @param histories
    *   The histories to be replayed
    * @param failFast
    *   If true, throws upon the first error encountered (if any) during replay. If false, all histories will be
    *   replayed and the returned object contains information about any failures.
    * @param worker
    *   A worker which should have registered all the workflow implementations which were used to produce (or are
    *   expected to be compatible with) the provided histories.
    * @return
    *   If `failFast` is false, contains any replay failures encountered.
    * @throws Exception
    *   If replay failed and `failFast` is true.
    */
  def replayWorkflowExecutions(
    histories: List[ZWorkflowExecutionHistory],
    failFast:  Boolean,
    worker:    ZWorker
  ): Task[ZReplayResults] =
    ZIO.attemptBlocking {
      val replayResults = WorkflowReplayer.replayWorkflowExecutions(
        histories.map(_.toJava).asJava,
        failFast,
        worker.toJava
      )
      ZReplayResults.fromJava(replayResults)
    }
}
