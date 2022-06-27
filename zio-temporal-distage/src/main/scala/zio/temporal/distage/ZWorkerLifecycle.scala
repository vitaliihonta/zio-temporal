package zio.temporal.distage

import zio._
import zio.temporal.worker.ZWorker
import zio.temporal.worker.ZWorkerFactory

/** A component which starts [[ZWorkerFactory]]
  */
class ZWorkerLifecycle(workerFactory: ZWorkerFactory, workers: Set[ZWorker]) {

  /** @return
    *   info about all the registered workers
    */
  def info: String = {
    val taskQueues = workers.map(_.toString).mkString("{", ", ", "}")
    s"Registered ${workers.size} workers listening to queues $taskQueues"
  }

  /** Starts the worker factory
    */
  def withWorkersStarted: UManaged[ZWorkerFactory] =
    ZManaged
      .make(
        workerFactory.start.as(workerFactory)
      )(_.shutdownNow)
}
