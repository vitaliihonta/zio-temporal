package zio.temporal.worker

import io.temporal.worker.WorkerFactory
import zio._
import zio.temporal.workflow.ZWorkflowClient

/** Maintains worker creation and lifecycle.
  *
  * @see
  *   [[WorkerFactory]]
  */
final class ZWorkerFactory private[zio] (val toJava: WorkerFactory) {

  /** Allows to run arbitrary effect ensuring a shutdown on effect completion.
    *
    * Shutdown will be initiated when effect either completes successfully or fails (with error or defect) The effect
    * will return after shutdown completed
    *
    * @param thunk
    *   the effect to run
    */
  def use[R, E, A](thunk: ZIO[R, E, A]): ZIO[R, E, A] =
    for {
      _ <- start
      result <- thunk.onExit { _ =>
                  shutdownNow
                }
    } yield result

  /** Starts all the workers created by this factory.
    */
  def start: UIO[Unit] =
    ZIO.blocking(ZIO.succeed(toJava.start()))

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * executed.
    *
    * @see
    *   [[WorkerFactory#shutdown]]
    */
  def shutdown: UIO[Unit] =
    ZIO.blocking(ZIO.succeed(toJava.shutdown()))

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * attempted to be stopped. This implementation cancels tasks via Thread.interrupt(), so any task that fails to
    * respond to interrupts may never terminate.
    *
    * @see
    *   [[WorkerFactory#shutdownNow]]
    */
  def shutdownNow: UIO[Unit] =
    ZIO.blocking(ZIO.succeed(toJava.shutdownNow()))

  /** Creates worker that connects to an instance of the Temporal Service. It uses the namespace configured at the
    * Factory level. New workers cannot be created after the start() has been called
    *
    * @see
    *   [[WorkerFactory#newWorker]]
    * @param taskQueue
    *   task queue name worker uses to poll. It uses this name for both workflow and activity task queue polls.
    * @param options
    *   Options for configuring worker.
    * @return
    *   ZWorker
    */
  def newWorker(taskQueue: String, options: ZWorkerOptions = ZWorkerOptions.default): UIO[ZWorker] =
    ZIO.succeed(new ZWorker(toJava.newWorker(taskQueue, options.toJava), Nil, Nil))

}

object ZWorkerFactory {

  /** Creates an instance of [[ZWorkerFactory]]
    *
    * @see
    *   [[WorkerFactory.newInstance]]
    *
    * @param client
    *   temporal client
    * @param options
    *   worker factory option
    * @return
    *   new worker factory
    */
  val make: URLayer[ZWorkflowClient with ZWorkerFactoryOptions, ZWorkerFactory] =
    ZLayer.fromZIO {
      ZIO.environmentWith[ZWorkflowClient with ZWorkerFactoryOptions] { environment =>
        new ZWorkerFactory(
          WorkerFactory.newInstance(
            environment.get[ZWorkflowClient].toJava,
            environment.get[ZWorkerFactoryOptions].toJava
          )
        )
      }
    }
}
