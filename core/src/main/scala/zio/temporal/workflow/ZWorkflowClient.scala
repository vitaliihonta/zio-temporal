package zio.temporal.workflow

import io.temporal.client.ActivityCompletionClient
import io.temporal.client.WorkflowClient
import zio.*
import zio.temporal.{ZWorkflowExecutionMetadata, experimentalApi}
import scala.jdk.OptionConverters.*
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

/** Represents temporal workflow client
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
    ZIO.blocking(ZIO.succeed(toJava.newActivityCompletionClient()))

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
        new ZWorkflowStubImpl(toJava.newUntypedWorkflowStub(workflowId, runId.toJava, Option.empty[String].toJava))
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

  /** TODO: should it use zio.stream?
    *
    * @see
    *   [[WorkflowClient.listExecutions]]
    */
  @experimentalApi
  def listExecutions(query: Option[String] = None): Task[Vector[ZWorkflowExecutionMetadata]] =
    ZIO.attempt(
      toJava
        .listExecutions(query.orNull)
        .iterator()
        .asScala
        .map(new ZWorkflowExecutionMetadata(_))
        .toVector
    )
}

object ZWorkflowClient {

  /** Create [[ZWorkflowClient]] instance
    * @see
    *   [[WorkflowClient]]
    */
  val make: URLayer[ZWorkflowServiceStubs with ZWorkflowClientOptions, ZWorkflowClient] =
    ZLayer.fromZIO {
      ZIO.environmentWithZIO[ZWorkflowServiceStubs with ZWorkflowClientOptions] { environment =>
        ZIO.blocking {
          ZIO.succeed {
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
}
