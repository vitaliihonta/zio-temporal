package zio.temporal.workflow

import io.temporal.client.{ActivityCompletionClient, WorkflowClient}
import zio.UIO
import zio.temporal.internal.TemporalInteraction
import zio.temporal.signal.{ZInput, ZSignal}
import zio.temporal.{TemporalClientError, TemporalIO, ZWorkflowExecution}

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
    UIO.effectTotal {
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
    signal: ZSignal[Any, ZSignal.SignalWithStart]
  ): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    signalWithStart[Any](signal)(())

  /** Invokes SignalWithStart operation.
    *
    * @param signal
    *   ZSignal to invoke (containing both @WorkflowMethod and @SignalMethod annotations
    * @param input
    *   ZSignal input
    * @return
    *   workflowExecution of the signaled or started workflow.
    */
  def signalWithStart[A](
    signal:             ZSignal[A, ZSignal.SignalWithStart]
  )(input:              A
  )(implicit inputFrom: ZInput.From[A]
  ): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    TemporalInteraction.from {
      val batchRequest = self.newSignalWithStartRequest()
      signal.addRequests(inputFrom(input), batchRequest)
      new ZWorkflowExecution(self.signalWithStart(batchRequest))
    }

  /** Creates new ActivityCompletionClient
    * @see
    *   [[ActivityCompletionClient]]
    */
  def newActivityCompletionClient: UIO[ActivityCompletionClient] =
    UIO.effectTotal(self.newActivityCompletionClient())

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
    *
    * @param service
    *   client to the Temporal Service endpoint.
    * @param options
    *   client option
    * @return
    *   \- workflow client instance
    * @see
    *   [[WorkflowClient]]
    */
  def make(
    service: ZWorkflowServiceStubs,
    options: ZWorkflowClientOptions = ZWorkflowClientOptions.default
  ): ZWorkflowClient =
    new ZWorkflowClient(WorkflowClient.newInstance(service.self, options.toJava))
}
