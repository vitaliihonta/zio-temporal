package ztemporal.workflow

import io.temporal.serviceclient.WorkflowServiceStubs
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import ztemporal.ZAwaitTerminationOptions

/** Initializes and holds gRPC blocking and future stubs.
  */
class ZWorkflowServiceStubs private[ztemporal] (private[ztemporal] val self: WorkflowServiceStubs) extends AnyVal {

  /** Allows to run arbitrary effect ensuring a shutdown on effect completion.
    *
    * Shutdown will be initiated when effect either completes successfully or fails (with error or defect)
    * The effect will return after shutdown completed
    *
    * @param options await options with polling interval and poll delay
    */
  def use[R, E, A](
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.default
  )(thunk:   ZIO[R, E, A]
  ): ZIO[R with Blocking with Clock, E, A] =
    for {
      awaitFiber <- awaitTermination(options).fork
      result <- thunk.onExit { _ =>
                  shutdownNow *> awaitFiber.join
                }
    } yield result

  /** Shutdowns client asynchronously allowing existing gRPC calls to finish
    */
  def shutdown: UIO[Unit] =
    IO.effectTotal(
      self.shutdown()
    )

  /** Shutdowns client immediately cancelling existing gRPC calls
    */
  def shutdownNow: UIO[Unit] =
    IO.effectTotal(
      self.shutdownNow()
    )

  /** Awaits for gRPC stubs shutdown up to the specified timeout.
    *
    * The shutdown has to be initiated through shutdown or shutdownNow.
    *
    * @param options await options with polling interval and poll delay
    */
  def awaitTermination(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.default
  ): URIO[Blocking with Clock, Unit] = {
    import zio.duration._
    blocking
      .blocking {
        IO.effectTotal(
          self.awaitTermination(options.pollTimeout.length, options.pollTimeout.unit)
        )
      }
      .repeat(Schedule.recurUntil[Boolean](identity) && Schedule.fixed(Duration.fromScala(options.pollDelay)))
      .unit
  }
}

object ZWorkflowServiceStubs {

  /** Create gRPC connection stubs using provided options.
    *
    * @param options workflow service stub options
    */
  def make(options: ZWorkflowServiceStubsOptions): UManaged[ZWorkflowServiceStubs] =
    ZManaged.make(UIO(new ZWorkflowServiceStubs(WorkflowServiceStubs.newInstance(options.toJava))))(_.shutdownNow)
}
