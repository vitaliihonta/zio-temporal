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

  /** Allows to setup [[ZWorkerFactory]] with guaranteed finalization.
    */
  def setup: URIO[Scope, Unit] =
    for {
      _ <- start
      _ <- ZIO.addFinalizer {
             shutdownNow
           }
    } yield ()

  /** Allows to setup [[ZWorkerFactory]] with guaranteed finalization. To be used in worker-only applications
    */
  def serve: URIO[Scope, Nothing] =
    setup *> ZIO.logInfo("ZWorkerFactory endlessly polling tasks...") *> ZIO.never

  /** Starts all the workers created by this factory.
    */
  def start: UIO[Unit] =
    ZIO.succeedBlocking(toJava.start()) *> ZIO.logInfo("ZWorkerFactory started")

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * executed.
    *
    * @see
    *   [[WorkerFactory#shutdown]]
    */
  def shutdown: UIO[Unit] =
    ZIO.logInfo("ZWorkerFactory shutdown initiated...") *>
      ZIO.succeedBlocking(toJava.shutdown())

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * attempted to be stopped. This implementation cancels tasks via Thread.interrupt(), so any task that fails to
    * respond to interrupts may never terminate.
    *
    * @see
    *   [[WorkerFactory#shutdownNow]]
    */
  def shutdownNow: UIO[Unit] =
    ZIO.logInfo("ZWorkerFactory shutdownNow initiated...") *>
      ZIO.succeedBlocking(toJava.shutdownNow())

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
    ZIO.succeedBlocking(
      new ZWorker(toJava.newWorker(taskQueue, options.toJava))
    )

  /** @param taskQueue
    *   task queue name to lookup an existing worker for
    * @return
    *   a worker created previously through [[newWorker]] for the given task queue.
    */
  def getWorker(taskQueue: String): UIO[Option[ZWorker]] =
    ZIO
      .attemptBlocking(new ZWorker(toJava.getWorker(taskQueue)))
      .refineToOrDie[IllegalArgumentException]
      .option
}

object ZWorkerFactory {

  /** Allows to setup [[ZWorkerFactory]] with guaranteed finalization.
    */
  def setup: URIO[ZWorkerFactory with Scope, Unit] =
    ZIO.serviceWithZIO[ZWorkerFactory](_.setup)

  /** Allows to setup [[ZWorkerFactory]] with guaranteed finalization. To be used in worker-only applications
    */
  def serve: URIO[ZWorkerFactory with Scope, Nothing] =
    ZIO.serviceWithZIO[ZWorkerFactory](_.serve)

  /** @see
    *   [[ZWorkerFactory.newWorker]]
    */
  def newWorker(taskQueue: String, options: ZWorkerOptions = ZWorkerOptions.default): URIO[ZWorkerFactory, ZWorker] =
    ZIO.serviceWithZIO[ZWorkerFactory](_.newWorker(taskQueue, options))

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
