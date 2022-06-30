package zio.temporal.workflow

import io.temporal.serviceclient.WorkflowServiceStubs
import zio._
import zio.temporal.ZAwaitTerminationOptions
import java.util.concurrent.TimeUnit

/** Initializes and holds gRPC blocking and future stubs.
  */
class ZWorkflowServiceStubs private[zio] (private[zio] val self: WorkflowServiceStubs) extends AnyVal {

  /** Allows to run arbitrary effect ensuring a shutdown on effect completion.
    *
    * Shutdown will be initiated when effect either completes successfully or fails (with error or defect) The effect
    * will return after shutdown completed
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  def use[R, E, A](
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.default
  )(thunk:   ZIO[R, E, A]
  ): ZIO[R, E, A] =
    for {
      awaitFiber <- awaitTermination(options).fork
      result <- thunk.onExit { _ =>
                  shutdownNow *> awaitFiber.join
                }
    } yield result

  /** Shutdowns client asynchronously allowing existing gRPC calls to finish
    */
  def shutdown: UIO[Unit] =
    ZIO.blocking(
      ZIO.succeed(
        self.shutdown()
      )
    )

  /** Shutdowns client immediately cancelling existing gRPC calls
    */
  def shutdownNow: UIO[Unit] =
    ZIO.blocking(
      ZIO.succeed(
        self.shutdownNow()
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
          self.awaitTermination(options.pollTimeout.toNanos, TimeUnit.NANOSECONDS)
        )
      }
      .repeat(Schedule.recurUntil[Boolean](identity) && Schedule.fixed(options.pollDelay))
      .unit
}

object ZWorkflowServiceStubs {

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
