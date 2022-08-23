package zio.temporal.worker

import zio._
import zio.temporal.internal.ClassTagUtils
import io.temporal.worker.Worker
import scala.reflect.ClassTag

/** Hosts activity and workflow implementations. Uses long poll to receive activity and workflow tasks and processes
  * them in a correspondent thread pool.
  */
class ZWorker private[zio] (
  private val self:       Worker,
  private val workflows:  List[Class[_]],
  private val activities: List[Class[_]]) {

  override def toString: String = {
    val workflowsInfo  = workflows.map(_.getName).mkString("[", ", ", "]")
    val activitiesInfo = activities.map(_.getName).mkString("[", ", ", "]")

    s"ZWorker(taskQueue=${self.getTaskQueue}, workflows=$workflowsInfo, activities=$activitiesInfo)"
  }

  /** Allows to add workflow to this worker
    */
  def addWorkflow[I]: ZWorker.AddWorkflowDsl[I] =
    new ZWorker.AddWorkflowDsl[I](this) // TODO: validate that it's a concrete class

  /** Registers activity implementation objects with a worker. An implementation object can implement one or more
    * activity types.
    *
    * @see
    *   [[Worker#registerActivitiesImplementations]]
    */
  def addActivityImplementation[A <: AnyRef](activity: A): ZWorker = {
    val cls = activity.getClass
    self.registerActivitiesImplementations(activity)
    new ZWorker(self, workflows, cls :: activities)
  }
}

object ZWorker {

  final class AddWorkflowDsl[I] private[zio] (private val worker: ZWorker) extends AnyVal {

    /** Registers workflow implementation classes with a worker. Can be called multiple times to add more types.
      *
      * @param ctg
      *   workflow interface class tag
      * @see
      *   [[Worker#registerWorkflowImplementationTypes]]
      */
    def fromClass(implicit ctg: ClassTag[I]): ZWorker =
      fromClass(ClassTagUtils.classOf[I])

    /** Registers workflow implementation classes with a worker. Can be called multiple times to add more types.
      * @param cls
      *   workflow interface class tag
      * @see
      *   [[Worker#registerWorkflowImplementationTypes]]
      */
    def fromClass(cls: Class[I]): ZWorker = {
      worker.self.registerWorkflowImplementationTypes(cls)
      new ZWorker(worker.self, cls :: worker.workflows, worker.activities)
    }

    /** Configures a factory to use when an instance of a workflow implementation is created. The only valid use for
      * this method is unit testing, specifically to instantiate mocks that implement child workflows. An example of
      * mocking a child workflow:
      *
      * @tparam A
      *   workflow interface implementation
      * @param factory
      *   should create a workflow implementation
      * @param ctg
      *   workflow interface class tag
      * @see
      *   [[Worker#addWorkflowImplementationFactory]]
      */
    def apply[A <: I](factory: => A)(implicit ctg: ClassTag[I]): ZWorker =
      apply(ClassTagUtils.classOf[I], () => factory)

    /** Configures a factory to use when an instance of a workflow implementation is created. The only valid use for
      * this method is unit testing, specifically to instantiate mocks that implement child workflows. An example of
      * mocking a child workflow:
      *
      * @param cls
      *   workflow interface class
      * @param factory
      *   should create a workflow implementation
      * @see
      *   [[Worker#addWorkflowImplementationFactory]]
      */
    def apply(cls: Class[I], factory: () => I): ZWorker = {
      worker.self.addWorkflowImplementationFactory[I](cls, () => factory())
      new ZWorker(worker.self, cls :: worker.workflows, worker.activities)
    }
  }
}
