package zio.temporal.worker

import zio._
import zio.temporal.internal.ClassTagUtils
import io.temporal.worker.Worker
import zio.temporal.activity.{ExtendsActivity, IsActivity, ZActivityImplementationObject}
import zio.temporal.workflow.{
  ExtendsWorkflow,
  HasPublicNullaryConstructor,
  IsConcreteClass,
  IsWorkflow,
  ZWorkflowImplementationClass
}
import io.temporal.worker.WorkerFactory

import scala.reflect.ClassTag

/** Hosts activity and workflow implementations. Uses long poll to receive activity and workflow tasks and processes
  * them in a correspondent thread pool.
  */
class ZWorker private[zio] (
  val toJava: Worker) {

  def taskQueue: String =
    toJava.getTaskQueue

  def isSuspended: UIO[Boolean] =
    ZIO.succeed(toJava.isSuspended)

  def suspendPolling: UIO[Unit] =
    ZIO.succeedBlocking(toJava.suspendPolling())

  def resumePolling: UIO[Unit] =
    ZIO.succeedBlocking(toJava.resumePolling())

  override def toString: String =
    toJava.toString
      .replace("Worker", "ZWorker")
      .replace("WorkerOptions", "ZWorkerOptions")
      .replace("{", "(")
      .replace("}", ")")

  /** Adds workflow to this worker
    */
  def addWorkflow[I: ExtendsWorkflow]: ZWorker.AddWorkflowDsl[I] =
    new ZWorker.AddWorkflowDsl[I](worker = this, options = ZWorkflowImplementationOptions.default)

  /** @see
    *   [[addWorkflowImplementations]]
    */
  def addWorkflowImplementations(
    workflowImplementationClass: ZWorkflowImplementationClass[_],
    moreClasses:                 ZWorkflowImplementationClass[_]*
  ): UIO[ZWorker] =
    addWorkflowImplementations((workflowImplementationClass +: moreClasses).toList)

  /** Registers workflow implementation classes with a worker. Can be called multiple times to add more types. A
    * workflow implementation class must implement at least one interface with a method annotated with
    * [[zio.temporal.workflowMethod]]. By default, the short name of the interface is used as a workflow type that this
    * worker supports.
    *
    * <p>Implementations that share a worker must implement different interfaces as a workflow type is identified by the
    * workflow interface, not by the implementation.
    *
    * @throws io.temporal.worker.TypeAlreadyRegisteredException
    *   if one of the workflow types is already registered
    */
  def addWorkflowImplementations(
    workflowImplementationClasses: List[ZWorkflowImplementationClass[_]]
  ): UIO[ZWorker] = ZIO.succeed {
    if (workflowImplementationClasses.nonEmpty) {
      toJava.registerWorkflowImplementationTypes(
        workflowImplementationClasses.map(_.runtimeClass): _*
      )
    }
    this
  }

  /** @see
    *   [[addWorkflowImplementations]]
    */
  def addWorkflowImplementations(
    options:                     ZWorkflowImplementationOptions,
    workflowImplementationClass: ZWorkflowImplementationClass[_],
    moreClasses:                 ZWorkflowImplementationClass[_]*
  ): UIO[ZWorker] =
    addWorkflowImplementations(options, (workflowImplementationClass +: moreClasses).toList)

  /** @see
    *   [[addWorkflowImplementations]]
    */
  def addWorkflowImplementations(
    options:                       ZWorkflowImplementationOptions,
    workflowImplementationClasses: List[ZWorkflowImplementationClass[_]]
  ): UIO[ZWorker] = ZIO.succeed {
    toJava.registerWorkflowImplementationTypes(
      options.toJava,
      workflowImplementationClasses.map(_.runtimeClass): _*
    )
    this
  }

  /** Registers activity implementation objects with a worker. An implementation object can implement one or more
    * activity types.
    *
    * @see
    *   [[Worker#registerActivitiesImplementations]]
    */
  def addActivityImplementation[A <: AnyRef: ExtendsActivity](activity: A): UIO[ZWorker] = ZIO.succeed {
    toJava.registerActivitiesImplementations(activity)
    this
  }

  /** @see
    *   [[addActivityImplementations]]
    */
  def addActivityImplementations(
    activityImplementationObject: ZActivityImplementationObject[_],
    moreObjects:                  ZActivityImplementationObject[_]*
  ): UIO[ZWorker] =
    addActivityImplementations((activityImplementationObject +: moreObjects).toList)

  /** Register activity implementation objects with a worker. An implementation object can implement one or more
    * activity types.
    *
    * <p>An activity implementation object must implement at least one interface annotated with
    * [[zio.temporal.activityInterface]]. Each method of the annotated interface becomes an activity type.
    *
    * <p>Implementations that share a worker must implement different interfaces as an activity type is identified by
    * the activity interface, not by the implementation.
    */
  def addActivityImplementations(
    activityImplementationObjects: List[ZActivityImplementationObject[_]]
  ): UIO[ZWorker] = ZIO.succeed {
    // safe to case as ZActivityImplementationObject type parameter is <: AnyRef
    // Note: cast is needed only for Scala 2.12
    toJava.registerActivitiesImplementations(activityImplementationObjects.map(_.value.asInstanceOf[AnyRef]): _*)
    this
  }

  /** Registers activity implementation objects with a worker. An implementation object can implement one or more
    * activity types.
    *
    * @see
    *   [[Worker#registerActivitiesImplementations]]
    */
  def addActivityImplementationService[A <: AnyRef: ExtendsActivity: Tag]: URIO[A, ZWorker] = {
    ZIO.serviceWithZIO[A] { activity =>
      addActivityImplementation[A](activity)
    }
  }
}

object ZWorker {

  type Add[+LowerR, -UpperR] = ZIOAspect[LowerR, UpperR, Nothing, Any, ZWorker, ZWorker]

  /** Adds workflow to this worker
    */
  def addWorkflow[I: ExtendsWorkflow]: ZWorker.AddWorkflowAspectDsl[I] =
    new AddWorkflowAspectDsl[I](options = ZWorkflowImplementationOptions.default)

  /** @see
    *   [[ZWorker.addWorkflowImplementations]]
    */
  def addWorkflowImplementations(
    workflowImplementationClass: ZWorkflowImplementationClass[_],
    moreClasses:                 ZWorkflowImplementationClass[_]*
  ): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addWorkflowImplementations(workflowImplementationClass, moreClasses: _*))
    }

  /** @see
    *   [[ZWorker.addWorkflowImplementations]]
    */
  def addWorkflowImplementations(
    workflowImplementationClasses: List[ZWorkflowImplementationClass[_]]
  ): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addWorkflowImplementations(workflowImplementationClasses))
    }

  /** @see
    *   [[addWorkflowImplementations]]
    */
  def addWorkflowImplementations(
    options:                     ZWorkflowImplementationOptions,
    workflowImplementationClass: ZWorkflowImplementationClass[_],
    moreClasses:                 ZWorkflowImplementationClass[_]*
  ): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addWorkflowImplementations(options, workflowImplementationClass, moreClasses: _*))
    }

  /** @see
    *   [[addWorkflowImplementations]]
    */
  def addWorkflowImplementations(
    options:                       ZWorkflowImplementationOptions,
    workflowImplementationClasses: List[ZWorkflowImplementationClass[_]]
  ): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addWorkflowImplementations(options, workflowImplementationClasses))
    }

  def addActivityImplementation[Activity <: AnyRef: ExtendsActivity](activity: Activity): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addActivityImplementation[Activity](activity))
    }

  /** @see
    *   [[ZWorker.addActivityImplementations]]
    */
  def addActivityImplementations(
    activityImplementationObject: ZActivityImplementationObject[_],
    moreObjects:                  ZActivityImplementationObject[_]*
  ): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addActivityImplementations(activityImplementationObject, moreObjects: _*))
    }

  /** @see
    *   [[ZWorker.addActivityImplementations]]
    */
  def addActivityImplementations(
    activityImplementationObjects: List[ZActivityImplementationObject[_]]
  ): ZWorker.Add[Nothing, Any] =
    new ZIOAspect[Nothing, Any, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addActivityImplementations(activityImplementationObjects))
    }

  /** Adds activities from the given [[ZLayer]]
    *
    * @param activitiesLayer
    *   the list of activity implementation objects as a [[ZLayer]]
    */
  def addActivityImplementationsLayer[R0, E0](
    activitiesLayer: ZLayer[R0, E0, List[ZActivityImplementationObject[_]]]
  ): ZIOAspect[Nothing, R0 with Scope, E0, Any, ZWorker, ZWorker] = {
    new ZIOAspect[Nothing, R0 with Scope, E0, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: R0 with Scope, E >: E0 <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] = {
        zio.flatMap { worker =>
          activitiesLayer.build.flatMap { env =>
            worker.addActivityImplementations(env.get[List[ZActivityImplementationObject[_]]])
          }
        }
      }
    }
  }

  /** Adds activity from the ZIO environment.
    */
  def addActivityImplementationService[Activity <: AnyRef: ExtendsActivity: Tag]: ZWorker.Add[Nothing, Activity] =
    new ZIOAspect[Nothing, Activity, Nothing, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: Activity, E >: Nothing <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap(_.addActivityImplementationService[Activity])
    }

  /** Adds activity from the given [[ZLayer]]
    *
    * @param layer
    *   the activity implementation object as a [[ZLayer]]
    */
  def addActivityImplementationLayer[R0, Activity <: AnyRef: ExtendsActivity: Tag, E0](
    layer: ZLayer[R0, E0, Activity]
  ): ZIOAspect[Nothing, R0 with Scope, E0, Any, ZWorker, ZWorker] =
    new ZIOAspect[Nothing, R0 with Scope, E0, Any, ZWorker, ZWorker] {
      override def apply[R >: Nothing <: R0 with Scope, E >: E0 <: Any, A >: ZWorker <: ZWorker](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] =
        zio.flatMap { worker =>
          layer.build.flatMap { env =>
            worker.addActivityImplementation(env.get[Activity])
          }
        }
    }

  /** Allows building workers using [[ZIOAspect]]
    */
  final class AddWorkflowAspectDsl[I: ExtendsWorkflow] private[zio] (
    options: ZWorkflowImplementationOptions) {

    /** Specifies workflow implementation options for the worker
      *
      * @param value
      *   custom workflow implementation options for a worker
      */
    def withOptions(value: ZWorkflowImplementationOptions): AddWorkflowAspectDsl[I] =
      new AddWorkflowAspectDsl[I](value)

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
          zio.flatMap(_.addWorkflow[I].withOptions(options).fromClass)
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
          zio.flatMap(_.addWorkflow[I].withOptions(options).fromClass(cls))
      }

    /** Configures a factory to use when an instance of a workflow implementation is created. The only valid use for
      * this method is unit testing, specifically to instantiate mocks that implement child workflows. An example of
      * mocking a child workflow:
      *
      * @tparam Workflow
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
          zio.flatMap(_.addWorkflow[I].withOptions(options).from(f))
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
          zio.flatMap(_.addWorkflow[I].withOptions(options).from(cls, f))
      }
  }

  /** Allows building workers
    */
  final class AddWorkflowDsl[I] private[zio] (worker: ZWorker, options: ZWorkflowImplementationOptions) {

    /** Specifies workflow implementation options for the worker
      *
      * @param value
      *   custom workflow implementation options for a worker
      */
    def withOptions(value: ZWorkflowImplementationOptions): AddWorkflowDsl[I] =
      new AddWorkflowDsl[I](worker, value)

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
        worker.toJava.registerWorkflowImplementationTypes(options.toJava, cls)
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
        worker.toJava.registerWorkflowImplementationFactory[I](cls, () => f(), options.toJava)
        worker
      }
  }
}
