package zio.temporal.worker

import zio.*
import zio.temporal.internal.ClassTagUtils
import io.temporal.worker.Worker
import zio.temporal.activity.IsActivity
import zio.temporal.workflow.{HasPublicNullaryConstructor, IsConcreteClass, IsWorkflow}
import io.temporal.worker.WorkerFactory
import scala.reflect.ClassTag

/** Hosts activity and workflow implementations. Uses long poll to receive activity and workflow tasks and processes
  * them in a correspondent thread pool.
  */
class ZWorker private[zio] (
  val toJava: Worker) {

  def taskQueue: String = toJava.getTaskQueue

  def isSuspended: UIO[Boolean] = ZIO.succeed(toJava.isSuspended)

  def suspendPolling: UIO[Unit] =
    ZIO.blocking(
      ZIO.succeed(
        toJava.suspendPolling()
      )
    )

  def resumePolling: UIO[Unit] =
    ZIO.blocking(
      ZIO.succeed(
        toJava.resumePolling()
      )
    )

  override def toString: String =
    toJava.toString
      .replace("Worker", "ZWorker")
      .replace("WorkerOptions", "ZWorkerOptions")
      .replace("{", "(")
      .replace("}", ")")

  /** Allows to add workflow to this worker
    */
  def addWorkflow[I: IsWorkflow]: ZWorker.AddWorkflowDsl[I] =
    new ZWorker.AddWorkflowDsl[I](this)

  /** Registers activity implementation objects with a worker. An implementation object can implement one or more
    * activity types.
    *
    * @see
    *   [[Worker#registerActivitiesImplementations]]
    */
  def addActivityImplementation[A <: AnyRef: IsActivity](activity: A): UIO[ZWorker] = ZIO.succeed {
    toJava.registerActivitiesImplementations(activity)
    this
  }

  /** Registers activity implementation objects with a worker. An implementation object can implement one or more
    * activity types.
    *
    * @see
    *   [[Worker#registerActivitiesImplementations]]
    */
  def addActivityImplementationService[A <: AnyRef: IsActivity: Tag]: URIO[A, ZWorker] = {
    ZIO.serviceWithZIO[A] { activity =>
      addActivityImplementation[A](activity)
    }
  }
}

object ZWorker {

  type Add[+LowerR, -UpperR] = ZIOAspect[LowerR, UpperR, Nothing, Any, ZWorker, ZWorker]

  def addWorkflow[I: IsWorkflow]: ZWorker.AddWorkflowAspectDsl[I] =
    new AddWorkflowAspectDsl[I](implicitly[IsWorkflow[I]])

  def addActivityImplementation[Activity <: AnyRef: IsActivity](activity: Activity): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addActivityImplementation[Activity](activity))
    }

  def addActivityImplementationService[Activity <: AnyRef: IsActivity: Tag]: ZWorker.Add[Nothing, Activity] =
    new ZIOAspect[Nothing, Activity, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Activity, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addActivityImplementationService[Activity])
    }

  /** Allows building workers using [[ZIOAspect]]
    */
  final class AddWorkflowAspectDsl[I] private[zio] (private val isWorkflow: IsWorkflow[I]) extends AnyVal {
    // for internal use only
    private implicit def _isWorkflow: IsWorkflow[I] = isWorkflow

    /** Registers workflow implementation classes with a worker. Can be called multiple times to add more types.
      *
      * @param ctg
      *   workflow interface class tag
      * @see
      *   [[Worker#registerWorkflowImplementationTypes]]
      */
    def fromClass(
      implicit ctg:                ClassTag[I],
      isConcreteClass:             IsConcreteClass[I],
      hasPublicNullaryConstructor: HasPublicNullaryConstructor[I]
    ): ZWorker.Add[Nothing, Any] =
      new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
        override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
          zio:            ZIO[R, E, A]
        )(implicit trace: Trace
        ): ZIO[R, E, A] =
          zio.flatMap(_.addWorkflow[I].fromClass)
      }

    /** Registers workflow implementation classes with a worker. Can be called multiple times to add more types.
      *
      * @param cls
      *   workflow interface class tag
      * @see
      *   [[Worker#registerWorkflowImplementationTypes]]
      */
    def fromClass(
      cls:                         Class[I]
    )(implicit isConcreteClass:    IsConcreteClass[I],
      hasPublicNullaryConstructor: HasPublicNullaryConstructor[I]
    ): ZWorker.Add[Nothing, Any] =
      new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
        override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
          zio:            ZIO[R, E, A]
        )(implicit trace: Trace
        ): ZIO[R, E, A] =
          zio.flatMap(_.addWorkflow[I].fromClass(cls))
      }

    /** Configures a factory to use when an instance of a workflow implementation is created. The only valid use for
      * this method is unit testing, specifically to instantiate mocks that implement child workflows. An example of
      * mocking a child workflow:
      *
      * @tparam A
      *   workflow interface implementation
      * @param f
      *   should create a workflow implementation
      * @param ctg
      *   workflow interface class tag
      * @see
      *   [[Worker#addWorkflowImplementationFactory]]
      */
    def from[Workflow <: I](f: => Workflow)(implicit ctg: ClassTag[I]): ZWorker.Add[Nothing, Any] =
      new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
        override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
          zio:            ZIO[R, E, A]
        )(implicit trace: Trace
        ): ZIO[R, E, A] =
          zio.flatMap(_.addWorkflow[I].from(f))
      }

    /** Configures a factory to use when an instance of a workflow implementation is created. The only valid use for
      * this method is unit testing, specifically to instantiate mocks that implement child workflows. An example of
      * mocking a child workflow:
      *
      * @param cls
      *   workflow interface class
      * @param f
      *   should create a workflow implementation
      * @see
      *   [[Worker#addWorkflowImplementationFactory]]
      */
    def from(cls: Class[I], f: () => I): ZWorker.Add[Nothing, Any] =
      new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
        override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
          zio:            ZIO[R, E, A]
        )(implicit trace: Trace
        ): ZIO[R, E, A] =
          zio.flatMap(_.addWorkflow[I].from(cls, f))
      }
  }

  /** Allows building workers
    */
  final class AddWorkflowDsl[I] private[zio] (private val worker: ZWorker) extends AnyVal {

    /** Registers workflow implementation classes with a worker. Can be called multiple times to add more types.
      *
      * @param ctg
      *   workflow interface class tag
      * @see
      *   [[Worker#registerWorkflowImplementationTypes]]
      */
    def fromClass(
      implicit ctg:                ClassTag[I],
      isConcreteClass:             IsConcreteClass[I],
      hasPublicNullaryConstructor: HasPublicNullaryConstructor[I]
    ): UIO[ZWorker] =
      fromClass(ClassTagUtils.classOf[I])

    /** Registers workflow implementation classes with a worker. Can be called multiple times to add more types.
      * @param cls
      *   workflow interface class tag
      * @see
      *   [[Worker#registerWorkflowImplementationTypes]]
      */
    def fromClass(
      cls:                         Class[I]
    )(implicit isConcreteClass:    IsConcreteClass[I],
      hasPublicNullaryConstructor: HasPublicNullaryConstructor[I]
    ): UIO[ZWorker] = {
      ZIO.succeed {
        worker.toJava.registerWorkflowImplementationTypes(cls)
        worker
      }
    }

    /** Configures a factory to use when an instance of a workflow implementation is created. The only valid use for
      * this method is unit testing, specifically to instantiate mocks that implement child workflows. An example of
      * mocking a child workflow:
      *
      * @tparam A
      *   workflow interface implementation
      * @param f
      *   should create a workflow implementation
      * @param ctg
      *   workflow interface class tag
      * @see
      *   [[Worker#addWorkflowImplementationFactory]]
      */
    def from[A <: I](f: => A)(implicit ctg: ClassTag[I]): UIO[ZWorker] =
      from(ClassTagUtils.classOf[I], () => f)

    /** Configures a factory to use when an instance of a workflow implementation is created. The only valid use for
      * this method is unit testing, specifically to instantiate mocks that implement child workflows. An example of
      * mocking a child workflow:
      *
      * @param cls
      *   workflow interface class
      * @param f
      *   should create a workflow implementation
      * @see
      *   [[Worker#addWorkflowImplementationFactory]]
      */
    def from(cls: Class[I], f: () => I): UIO[ZWorker] =
      ZIO.succeed {
        worker.toJava.registerWorkflowImplementationFactory[I](cls, () => f())
        worker
      }
  }
}
