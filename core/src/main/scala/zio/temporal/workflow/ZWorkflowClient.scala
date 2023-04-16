package zio.temporal.workflow

import io.temporal.client.ActivityCompletionClient
import io.temporal.client.WorkflowClient
import zio.*
import zio.temporal.internalApi
import zio.temporal.signal.ZWorkflowClientSignalWithStartSyntax

import scala.compat.java8.OptionConverters.*
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

  /** Creates new type workflow stub builder
    * @tparam A
    *   workflow interface
    * @return
    *   builder instance
    */
  def newWorkflowStub[A: ClassTag: IsWorkflow]: ZWorkflowStubBuilderTaskQueueDsl[A] =
    new ZWorkflowStubBuilderTaskQueueDsl[A](toJava)

  def newWorkflowStubProxy[A: ClassTag: IsWorkflow](
    workflowId: String,
    runId:      Option[String] = None
  ): UIO[ZWorkflowStub.Of[A]] =
    ZIO.succeed {
      ZWorkflowStub.Of[A](
        new ZWorkflowStubImpl(toJava.newUntypedWorkflowStub(workflowId, runId.asJava, Option.empty[String].asJava))
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
