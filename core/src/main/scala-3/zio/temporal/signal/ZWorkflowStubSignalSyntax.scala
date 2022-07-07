package zio.temporal.signal

import io.temporal.client.BatchRequest
import zio.temporal.internalApi
import zio.temporal.workflow.ZWorkflowClient

// TODO: implement
trait ZWorkflowStubSignalSyntax {}

// TODO: implement
trait ZWorkflowClientSignalWithStartSyntax extends Any {}

// TODO: implement
final class ZSignalBuilder @internalApi() (
  val __zio_temporal_workflowClient: ZWorkflowClient,
  val __zio_temporal_addSignal:      BatchRequest => Unit)
