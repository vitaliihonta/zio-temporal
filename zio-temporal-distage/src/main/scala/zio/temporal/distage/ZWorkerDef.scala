package zio.temporal.distage

import distage.Functoid
import izumi.distage.model.definition.ModuleDef
import izumi.distage.model.definition.dsl.ModuleDefDSL.MakeDSL
import izumi.distage.model.definition.dsl.ModuleDefDSL.SetElementDSL
import izumi.fundamentals.platform.language.CodePositionMaterializer
import zio.temporal.distage.internal.WorkerDefMacro
import zio.temporal.internalApi
import zio.temporal.worker.ZWorker
import zio.temporal.worker.ZWorkerFactory

import scala.language.experimental.macros

/** Distage module for creating [[ZWorker]]s
  *
  * @param taskQueue
  *   the task queue this worker would listen to
  */
abstract class ZWorkerDef(val taskQueue: String) extends ModuleDef {

  /** '''WARNING''' should not be used outside of this [[ZWorkerDef]] A helper type tag
    */
  protected[this] sealed trait ThisWorkerTag extends Any

  /** '''WARNING''' should not be used outside of this [[ZWorkerDef]] A type ensuring that the given activity or
    * workflow was registered into this worker.
    *
    * @tparam F
    *   either [[RegisteredActivity]] or [[RegisteredWorkflow]]
    */
  protected[this] final type Mine[F[_]] = F[AnyRef] with ThisWorkerTag

  /** '''WARNING''' should not be used directly Used during the macro expansion
    */
  @internalApi
  protected[this] def Mine[F[_], A](value: F[A]): Mine[F] = value.asInstanceOf[Mine[F]]

  /** Entry-point for registering the workflow implementation.
    *
    * Ensures that the given type is a workflow implementation (it's a child type of any interface with
    * [[io.temporal.workflow.WorkflowInterface]] annotation)
    * @tparam A
    *   workflow implementation
    */
  protected[this] def registerWorkflowImplementation[A]: SetElementDSL[Mine[RegisteredWorkflow]] =
    macro WorkerDefMacro.registerWorkflowImplementationImpl[A]

  /** Entry-point for registering the workflow implementation.
    *
    * Ensures that the given type is a workflow implementation (it's a child type of any interface with
    * [[io.temporal.workflow.WorkflowInterface]] annotation)
    * @tparam A
    *   workflow implementation
    */
  protected[this] def registerWorkflow[A]: MakeWorkflowImplementationDsl[A] =
    macro WorkerDefMacro.registerWorkflowFactoryImpl[A]

  /** Entry-point for registering the activity implementation.
    *
    * Ensures that the given type is a activity interface (it's a child type of any interface with
    * [[io.temporal.activity.ActivityInterface]] annotation)
    * @tparam A
    *   workflow implementation
    */
  protected[this] def registerActivity[A]: MakeDSL[A] = macro WorkerDefMacro.registerActivityImpl[A]

  protected[this] final class MakeWorkflowImplementationDsl[A] @internalApi() (cls: Class[A]) {

    /** Creates a workflow implementation
      */
    def fromFactory(
      f:            Functoid[ZWorkflowFactory[A]]
    )(implicit pos: CodePositionMaterializer
    ): SetElementDSL[Mine[RegisteredWorkflow]] =
      many[Mine[RegisteredWorkflow]]
        .add(f.map(factory => Mine(new RegisteredWorkflow[A](cls, Some(factory)))))
  }

  many[ZWorker].addEffect {
    (
      workerFactory: ZWorkerFactory,
      workflows:     Set[Mine[RegisteredWorkflow]],
      activities:    Set[Mine[RegisteredActivity]]
    ) =>
      workerFactory.newWorker(taskQueue).map { worker =>
        val base = workflows.foldLeft(worker) { (w, wf) =>
          wf.factoryOpt
            .fold(ifEmpty = w.addWorkflow.fromClass(wf.cls))(factory => w.addWorkflow(wf.cls, factory))
        }
        activities.foldLeft(base)((w, act) => w.addActivityImplementation(act.activity))
      }
  }
}
