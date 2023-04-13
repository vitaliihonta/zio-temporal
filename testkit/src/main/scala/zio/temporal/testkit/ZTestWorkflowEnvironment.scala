package zio.temporal.testkit

import io.temporal.testing.TestWorkflowEnvironment
import zio.*
import zio.temporal.ZAwaitTerminationOptions
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker.ZWorker
import zio.temporal.worker.ZWorkerOptions
import zio.temporal.workflow.ZWorkflowClient
import zio.temporal.workflow.ZWorkflowServiceStubs

import java.util.concurrent.TimeUnit

/** TestWorkflowEnvironment provides workflow unit testing capabilities.
  *
  * <p>Testing the workflow code is hard as it might be potentially very long running. The included in-memory
  * implementation of the Temporal service supports <b>an automatic time skipping</b>. Anytime a workflow under the test
  * as well as the unit test code are waiting on a timer (or sleep) the internal service time is automatically advanced
  * to the nearest time that unblocks one of the waiting threads. This way a workflow that runs in production for months
  * is unit tested in milliseconds. Here is an example of a test that executes in a few milliseconds instead of over two
  * hours that are needed for the workflow to complete:
  *
  * @see
  *   [[TestWorkflowEnvironment]]
  */
class ZTestWorkflowEnvironment[R] private[zio] (val toJava: TestWorkflowEnvironment, runtime: zio.Runtime[R]) {

  /** Creates a new Worker instance that is connected to the in-memory test Temporal service.
    *
    * @param taskQueue
    *   task queue to poll.
    */
  def newWorker(taskQueue: String, options: ZWorkerOptions = ZWorkerOptions.default): UIO[ZWorker] =
    ZIO.blocking(
      ZIO.succeed(
        new ZWorker(toJava.newWorker(taskQueue, options.toJava))
      )
    )

  /** Creates a WorkflowClient that is connected to the in-memory test Temporal service. */
  lazy val workflowClient = new ZWorkflowClient(toJava.getWorkflowClient)

  /** Returns the in-memory test Temporal service that is owned by this. */
  lazy val workflowService = new ZWorkflowServiceStubs(toJava.getWorkflowServiceStubs)

  implicit lazy val activityOptions: ZActivityOptions[R] =
    new ZActivityOptions[R](runtime, workflowClient.toJava.newActivityCompletionClient())

  /** Allows to run arbitrary effect ensuring a shutdown on effect completion.
    *
    * Shutdown will be initiated when effect either completes successfully or fails (with error or defect) The effect
    * will return after shutdown completed
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  @deprecated("Use scope-based 'setup' or explicit start/shutdownNow", since = "0.2.0")
  def use[E, A](
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault
  )(thunk:   ZIO[R, E, A]
  ): ZIO[R, E, A] =
    for {
      started <- start.fork
      result <- thunk.onExit { _ =>
                  workflowService.shutdownNow.fork.zipWith(shutdownNow.fork) { (stubsShutdown, thisShutdown) =>
                    awaitTermination(options).fork.flatMap { awaitFiber =>
                      Fiber.joinAll(
                        List(stubsShutdown, thisShutdown, awaitFiber, started)
                      )
                    }
                  }
                }
    } yield result

  /** Setup test environment with a guaranteed finalization.
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  def setup(options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault): URIO[Scope, Unit] =
    for {
      started <- start.fork
      _ <- ZIO.addFinalizer {
             shutdownNow *> awaitTermination(options) *> started.join
           }
      _ <- workflowService.setup()
    } yield ()

  /** Start all workers created by this test environment. */
  def start: UIO[Unit] =
    ZIO.blocking(ZIO.succeed(toJava.start()))

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * executed.
    *
    * @see
    *   [[TestWorkflowEnvironment#shutdown]]
    */
  def shutdown: UIO[Unit] =
    ZIO.blocking(ZIO.succeed(toJava.shutdown()))

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * attempted to be stopped. This implementation cancels tasks via Thread.interrupt(), so any task that fails to
    * respond to interrupts may never terminate.
    *
    * @see
    *   [[TestWorkflowEnvironment#shutdownNow]]
    */
  def shutdownNow: UIO[Unit] =
    ZIO.blocking(ZIO.succeed(toJava.shutdownNow()))

  /** Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current
    * thread is interrupted, whichever happens first.
    *
    * @param options
    *   await termination options
    */
  def awaitTermination(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault
  ): UIO[Unit] =
    ZIO
      .blocking {
        ZIO.succeed(
          toJava.awaitTermination(options.pollTimeout.toNanos, TimeUnit.NANOSECONDS)
        )
      }
      .repeat(Schedule.recurUntil((_: Unit) => true) && Schedule.fixed(options.pollDelay))
      .unit
}

object ZTestWorkflowEnvironment {

  /** Setup test environment with a guaranteed finalization.
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  def setup[R: Tag](
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault
  ): URIO[ZTestWorkflowEnvironment[R] with Scope, Unit] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[R]](_.setup(options))

  /** Creates a new instance of [[ZTestWorkflowEnvironment]]
    *
    * @see
    *   [[TestWorkflowEnvironment.newInstance]]
    * @return
    *   managed instance of test environment
    */
  def make[R: Tag]: URLayer[R with ZTestEnvironmentOptions, ZTestWorkflowEnvironment[R]] =
    ZLayer.scoped[R with ZTestEnvironmentOptions] {
      ZIO.runtime[R with ZTestEnvironmentOptions].flatMap { runtime =>
        ZIO.blocking(
          ZIO.succeed(
            new ZTestWorkflowEnvironment[R](
              TestWorkflowEnvironment.newInstance(runtime.environment.get[ZTestEnvironmentOptions].toJava),
              runtime
            )
          )
        )
      }
    }
}
