package zio.temporal.workflow

import io.temporal.client.{ActivityCompletionClient, BuildIdOperation, WorkflowClient}
import zio._
import zio.stream._
import zio.temporal.internal.{ClassTagUtils, TemporalInteraction}
import zio.temporal.{TemporalIO, ZHistoryEvent, ZWorkflowExecutionHistory, ZWorkflowExecutionMetadata}

import scala.jdk.OptionConverters._
import scala.reflect.ClassTag

/** Represents Temporal workflow client
  *
  * @see
  *   [[WorkflowClient]]
  */
final class ZWorkflowClient private[zio] (val toJava: WorkflowClient) {

  /** Creates new ActivityCompletionClient
    * @see
    *   [[ActivityCompletionClient]]
    */
  def newActivityCompletionClient: UIO[ActivityCompletionClient] =
    ZIO.succeedBlocking(toJava.newActivityCompletionClient())

  /** Creates new typed workflow stub builder
    * @tparam A
    *   workflow interface
    * @return
    *   builder instance
    */
  def newWorkflowStub[A: ClassTag: IsWorkflow]: ZWorkflowStubBuilderTaskQueueDsl.Of[A] =
    new ZWorkflowStubBuilderTaskQueueDsl.Of[A](ZWorkflowStubBuilderTaskQueueDsl.typed[A](toJava))

  /** Creates new untyped type workflow stub builder
    *
    * @param workflowType
    *   name of the workflow type
    * @return
    *   builder instance
    */
  def newUntypedWorkflowStub(workflowType: String): ZWorkflowStubBuilderTaskQueueDsl.Untyped =
    new ZWorkflowStubBuilderTaskQueueDsl.Untyped(ZWorkflowStubBuilderTaskQueueDsl.untyped(workflowType, toJava))

  def newWorkflowStub[A: ClassTag: IsWorkflow](
    workflowId: String,
    runId:      Option[String] = None
  ): UIO[ZWorkflowStub.Of[A]] =
    ZIO.succeed {
      ZWorkflowStub.Of[A](
        new ZWorkflowStubImpl(
          toJava.newUntypedWorkflowStub(workflowId, runId.toJava, Option.empty[String].toJava),
          ClassTagUtils.classOf[A]
        )
      )
    }

  def newUntypedWorkflowStub(
    workflowId: String,
    runId:      Option[String]
  ): UIO[ZWorkflowStub.Untyped] =
    ZIO.succeed {
      new ZWorkflowStub.UntypedImpl(
        toJava.newUntypedWorkflowStub(workflowId, runId.toJava, Option.empty[String].toJava)
      )
    }

  /** A wrapper around {WorkflowServiceStub#listWorkflowExecutions(ListWorkflowExecutionsRequest)}
    *
    * @param query
    *   Temporal Visibility Query, for syntax see <a href="https://docs.temporal.io/visibility#list-filter">Visibility
    *   docs</a>
    * @return
    *   sequential stream that performs remote pagination under the hood
    */
  def streamExecutions(query: Option[String] = None): Stream[Throwable, ZWorkflowExecutionMetadata] = {
    ZStream
      .blocking(
        ZStream.fromJavaStreamZIO(
          ZIO.attempt(
            toJava.listExecutions(query.orNull)
          )
        )
      )
      .map(new ZWorkflowExecutionMetadata(_))
  }

  /** Streams history events for a workflow execution for the provided `workflowId`.
    *
    * @param workflowId
    *   Workflow Id of the workflow to export the history for
    * @param runId
    *   Fixed Run Id of the workflow to export the history for. If not provided, the latest run will be used. Optional
    * @return
    *   stream of history events of the specified run of the workflow execution.
    * @see
    *   [[fetchHistory]] for a user-friendly eager version of this method
    */
  def streamHistory(workflowId: String, runId: Option[String] = None): Stream[Throwable, ZHistoryEvent] =
    ZStream
      .blocking(
        ZStream.fromJavaStreamZIO(
          ZIO.attempt(
            toJava.streamHistory(workflowId, runId.orNull)
          )
        )
      )
      .map(new ZHistoryEvent(_))

  /** Downloads workflow execution history for the provided `workflowId`.
    *
    * @param workflowId
    *   Workflow Id of the workflow to export the history for
    * @param runId
    *   Fixed Run Id of the workflow to export the history for. If not provided, the latest run will be used. Optional
    * @return
    *   execution history of the workflow with the specified Workflow Id.
    * @see
    *   [[streamHistory]] for a lazy memory-efficient version of this method
    */
  def fetchHistory(workflowId: String, runId: Option[String] = None): Task[ZWorkflowExecutionHistory] =
    ZIO
      .attemptBlocking(toJava.fetchHistory(workflowId, runId.orNull))
      .map(new ZWorkflowExecutionHistory(_))

  /** Allows you to update the worker-build-id based version sets for a particular task queue. This is used in
    * conjunction with workers who specify their build id and thus opt into the feature.
    *
    * @param taskQueue
    *   The task queue to update the version set(s) of.
    * @param operation
    *   The operation to perform. See [[BuildIdOperation]] for more.
    * @throws io.temporal.client.WorkflowServiceException
    *   for any failures including networking and service availability issues.
    * @note
    *   experimental in Java SDK
    */
  def updateWorkerBuildIdCompatibility(taskQueue: String, operation: BuildIdOperation): TemporalIO[Unit] =
    TemporalInteraction.from {
      toJava.updateWorkerBuildIdCompatability(taskQueue, operation)
    }

  /** Returns the worker-build-id based version sets for a particular task queue.
    *
    * @param taskQueue
    *   The task queue to fetch the version set(s) of.
    * @return
    *   The version set(s) for the task queue.
    * @throws io.temporal.client.WorkflowServiceException
    *   for any failures including networking and service availability issues.
    * @note
    *   experimental in Java SDK
    */
  def getWorkerBuildIdCompatibility(taskQueue: String): TemporalIO[ZWorkerBuildIdVersionSets] =
    TemporalInteraction.from {
      new ZWorkerBuildIdVersionSets(
        toJava.getWorkerBuildIdCompatability(taskQueue)
      )
    }
}

object ZWorkflowClient {

  /** Create [[ZWorkflowClient]] instance
    * @see
    *   [[WorkflowClient]]
    */
  val make: URLayer[ZWorkflowServiceStubs with ZWorkflowClientOptions, ZWorkflowClient] =
    ZLayer.fromZIO {
      ZIO.environmentWithZIO[ZWorkflowServiceStubs with ZWorkflowClientOptions] { environment =>
        ZIO.succeedBlocking {
          new ZWorkflowClient(
            WorkflowClient.newInstance(
              environment.get[ZWorkflowServiceStubs].toJava,
              environment.get[ZWorkflowClientOptions].toJava
            )
          )
        }
      }
    }
}
