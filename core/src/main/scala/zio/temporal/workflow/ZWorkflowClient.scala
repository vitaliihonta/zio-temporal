package zio.temporal.workflow

import io.temporal.client.ActivityCompletionClient
import io.temporal.client.WorkflowClient
import zio._
import zio.temporal.signal.ZWorkflowClientSignalWithStartSyntax
import scala.compat.java8.OptionConverters._
import scala.reflect.ClassTag

/** Represents temporal workflow client
  *
  * @see
  *   [[WorkflowClient]]
  */
class ZWorkflowClient private[zio] (val toJava: WorkflowClient)
    extends AnyVal
    with ZWorkflowClientSignalWithStartSyntax {

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
  def newWorkflowStub[A: ClassTag: IsConcreteType]: ZWorkflowStubBuilderTaskQueueDsl[A] =
    new ZWorkflowStubBuilderTaskQueueDsl[A](toJava, implicitly[ClassTag[A]])

  def newWorkflowStubProxy[A: ClassTag: IsConcreteType](
    workflowId: String,
    runId:      Option[String] = None
  ): UIO[ZWorkflowStub.Proxy[A]] =
    ZIO.succeed {
      ZWorkflowStub.Proxy[A](
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
                environment.get[ZWorkflowServiceStubs].self,
                environment.get[ZWorkflowClientOptions].toJava
              )
            )
          }
        }
      }
    }
}
