package zio.temporal.workflow

import io.temporal.client.ActivityCompletionClient
import io.temporal.client.WorkflowClient
import zio._
import zio.temporal.internal.TemporalInteraction
//import zio.temporal.signal.ZInput
import zio.temporal.signal.ZSignal
import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import scala.compat.java8.OptionConverters._
import scala.reflect.ClassTag

/** Represents temporal workflow client
  *
  * @see
  *   [[WorkflowClient]]
  */
class ZWorkflowClient private[zio] (private[zio] val self: WorkflowClient) extends AnyVal {

  /** Creates workflow untyped client stub for a known execution. Use it to send signals or queries to a running
    * workflow. Do not call methods annotated with @WorkflowMethod.
    *
    * @see
    *   [[ZWorkflowStub]]
    */
  def newUntypedWorkflowStub(workflowId: String, runId: Option[String] = None): UIO[ZWorkflowStub] =
    ZIO.succeed {
      new ZWorkflowStub(self.newUntypedWorkflowStub(workflowId, runId.asJava, Option.empty[String].asJava))
    }

  /** Invokes SignalWithStart operation.
    *
    * @param signal
    *   ZSignal to invoke (containing both @WorkflowMethod and @SignalMethod annotations
    * @return
    *   workflowExecution of the signaled or started workflow.
    */
  def signalWithStart(
    signal: ZSignal.WithStart
  ): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    TemporalInteraction.from {
      val batchRequest = self.newSignalWithStartRequest()
      signal.addRequests(batchRequest)
      new ZWorkflowExecution(self.signalWithStart(batchRequest))
    }

  /** Creates new ActivityCompletionClient
    * @see
    *   [[ActivityCompletionClient]]
    */
  def newActivityCompletionClient: UIO[ActivityCompletionClient] =
    ZIO.blocking(ZIO.succeed(self.newActivityCompletionClient()))

  /** Creates new type workflow stub builder
    * @tparam A
    *   workflow interface
    * @return
    *   builder instance
    */
  def newWorkflowStub[A: ClassTag]: ZWorkflowStubBuilderTaskQueueDsl[A] =
    new ZWorkflowStubBuilderTaskQueueDsl[A](self, implicitly[ClassTag[A]])
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
