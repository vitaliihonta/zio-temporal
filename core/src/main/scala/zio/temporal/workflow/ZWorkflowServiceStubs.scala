package zio.temporal.workflow

import io.temporal.serviceclient.WorkflowServiceStubs
import zio._
import zio.temporal.ZAwaitTerminationOptions
import java.util.concurrent.TimeUnit

/** Initializes and holds gRPC blocking and future stubs.
  */
final class ZWorkflowServiceStubs private[zio] (val toJava: WorkflowServiceStubs) {

  /** Allows to setup [[ZWorkflowServiceStubs]] with guaranteed finalization.
    */
  def setup(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.default
  ): URIO[Scope, Unit] =
    ZIO.addFinalizer {
      shutdownNow *> awaitTermination(options)
    }.unit

  /** Shutdowns client asynchronously allowing existing gRPC calls to finish
    */
  def shutdown: UIO[Unit] =
    ZIO.blocking(
      ZIO.succeed(
        toJava.shutdown()
      )
    )

  /** Shutdowns client immediately cancelling existing gRPC calls
    */
  def shutdownNow: UIO[Unit] =
    ZIO.blocking(
      ZIO.succeed(
        toJava.shutdownNow()
      )
    )

  /** Awaits for gRPC stubs shutdown up to the specified timeout.
    *
    * The shutdown has to be initiated through shutdown or shutdownNow.
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  def awaitTermination(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.default
  ): UIO[Unit] =
    ZIO
      .blocking {
        ZIO.succeed(
          toJava.awaitTermination(options.pollTimeout.toNanos, TimeUnit.NANOSECONDS)
        )
      }
      .repeat(Schedule.recurUntil[Boolean](identity) && Schedule.fixed(options.pollDelay))
      .unit
}

object ZWorkflowServiceStubs {

  /** Allows to setup [[ZWorkflowServiceStubs]] with guaranteed finalization.
    */
  def setup(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.default
  ): URIO[ZWorkflowServiceStubs with Scope, Unit] =
    ZIO.serviceWithZIO[ZWorkflowServiceStubs](_.setup(options))

  /** Create gRPC connection stubs using provided options.
    */
  val make: URLayer[ZWorkflowServiceStubsOptions, ZWorkflowServiceStubs] = ZLayer.scoped {
    ZIO.serviceWithZIO[ZWorkflowServiceStubsOptions] { options =>
      ZIO.acquireRelease(
        ZIO.blocking(
          ZIO.succeed(
            new ZWorkflowServiceStubs(WorkflowServiceStubs.newServiceStubs(options.toJava))
          )
        )
      )(_.shutdownNow)
    }
  }
}
