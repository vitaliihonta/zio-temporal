package zio.temporal.signal

import io.temporal.client.BatchRequest
import zio.temporal.internal.ZSignalMacro
import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internalApi
import zio.temporal.workflow.ZWorkflowClient
import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowStubSignalSyntax {
  def signal(f: Unit): TemporalIO[TemporalClientError, Unit] =
    macro ZSignalMacro.signalImpl
}

trait ZWorkflowClientSignalWithStartSyntax extends Any {

  /** Creates builder for SignalWithStart operation.
    *
    * @param f
    *   signal method call
    * @return
    *   the builder
    */
  def signalWith(f: Unit): ZSignalBuilder =
    macro ZSignalMacro.signalWithStartBuilderImpl
}

final class ZSignalBuilder @internalApi() (
  val __zio_temporal_workflowClient: ZWorkflowClient,
  val __zio_temporal_addSignal:      BatchRequest => Unit) {

  /** Invokes SignalWithStart operation.
    *
    * @param f
    *   workflow method to start
    * @return
    *   workflowExecution of the started and signaled workflow.
    */
  def start[A](f: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    macro ZSignalMacro.signalWithStartImpl[A]
}
