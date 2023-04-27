package zio.temporal.workflow

import io.temporal.client.ActivityCompletionClient
import io.temporal.client.WorkflowClient
import zio.*
import zio.temporal.internalApi
import scala.jdk.OptionConverters.*
import scala.reflect.ClassTag

/** Represents temporal workflow client
  *
  * @see
  *   [[WorkflowClient]]
  */
final class ZWorkflowClient @internalApi() (val toJava: WorkflowClient) {

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
  def newWorkflowStub[A: ClassTag: IsWorkflowInterface]: ZWorkflowStubBuilderTaskQueueDsl.Of[A] =
    new ZWorkflowStubBuilderTaskQueueDsl.Of[A](toJava, ZWorkflowStubBuilderTaskQueueDsl.typed[A])

  /** Creates new untyped type workflow stub builder
    *
    * @param workflowType
    *   name of the workflow type
    * @return
    *   builder instance
    */
  def newUntypedWorkflowStub(workflowType: String): ZWorkflowStubBuilderTaskQueueDsl.Untyped =
    new ZWorkflowStubBuilderTaskQueueDsl.Untyped(toJava, ZWorkflowStubBuilderTaskQueueDsl.untyped(workflowType))

  def newWorkflowStub[A: ClassTag: IsWorkflowInterface](
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
