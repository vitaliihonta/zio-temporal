package zio.temporal.testkit

import io.temporal.testing.TestWorkflowEnvironment
import zio._
import zio.blocking.Blocking
import zio.clock.Clock
import zio.temporal.ZAwaitTerminationOptions
import zio.temporal.worker.{ZWorker, ZWorkerOptions}
import zio.temporal.workflow.{ZWorkflowClient, ZWorkflowServiceStubs}

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
class ZTestWorkflowEnvironment private[zio] (private val self: TestWorkflowEnvironment) extends AnyVal {

  /** Creates a new Worker instance that is connected to the in-memory test Temporal service.
    *
    * @param taskQueue
    *   task queue to poll.
    */
  def newWorker(taskQueue: String, options: ZWorkerOptions = ZWorkerOptions.default) =
    new ZWorker(self.newWorker(taskQueue, options.toJava), workflows = Nil, activities = Nil)

  /** Creates a WorkflowClient that is connected to the in-memory test Temporal service. */
  def workflowClient = new ZWorkflowClient(self.getWorkflowClient)

  /** Returns the in-memory test Temporal service that is owned by this. */
  def workflowService = new ZWorkflowServiceStubs(self.getWorkflowService)

  /** Allows to run arbitrary effect ensuring a shutdown on effect completion.
    *
    * Shutdown will be initiated when effect either completes successfully or fails (with error or defect) The effect
    * will return after shutdown completed
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  def use[R, E, A](
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault
  )(thunk:   ZIO[R, E, A]
  ): ZIO[R with Blocking with Clock, E, A] =
    for {
      started    <- start.fork
      awaitFiber <- awaitTermination(options).fork
      result <- thunk.onExit { _ =>
                  workflowService.shutdownNow.fork.zipWith(shutdownNow.fork) { (stubsShutdown, thisShutdown) =>
                    Fiber.joinAll(
                      List(stubsShutdown, thisShutdown, awaitFiber, started)
                    )
                  }
                }
    } yield result

  /** Start all workers created by this test environment. */
  def start: UIO[Unit] =
    UIO.effectTotal(self.start())

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * executed.
    *
    * @see
    *   [[TestWorkflowEnvironment#shutdown]]
    */
  def shutdown: UIO[Unit] =
    UIO.effectTotal(self.shutdown())

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * attempted to be stopped. This implementation cancels tasks via Thread.interrupt(), so any task that fails to
    * respond to interrupts may never terminate.
    *
    * @see
    *   [[TestWorkflowEnvironment#shutdownNow]]
    */
  def shutdownNow: UIO[Unit] =
    UIO.effectTotal(self.shutdownNow())

  /** Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current
    * thread is interrupted, whichever happens first.
    *
    * @param options
    *   await termination options
    */
  def awaitTermination(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault
  ): URIO[Blocking with Clock, Unit] = {
    import zio.duration._
    blocking
      .blocking {
        IO.effectTotal(
          self.awaitTermination(options.pollTimeout.length, options.pollTimeout.unit)
        )
      }
      .repeat(Schedule.recurUntil((_: Unit) => true) && Schedule.fixed(Duration.fromScala(options.pollDelay)))
      .unit
  }
}

object ZTestWorkflowEnvironment {

  /** Creates a new instance of [[ZTestWorkflowEnvironment]]
    *
    * @see
    *   [[TestWorkflowEnvironment.newInstance]]
    * @param options
    *   test environment options
    * @return
    *   managed instance of test environment
    */
  def make(options: ZTestEnvironmentOptions = ZTestEnvironmentOptions.default): UManaged[ZTestWorkflowEnvironment] =
    ZManaged.make(
      IO.effectTotal(
        new ZTestWorkflowEnvironment(TestWorkflowEnvironment.newInstance(options.toJava))
      )
    )(_.shutdown)
}
