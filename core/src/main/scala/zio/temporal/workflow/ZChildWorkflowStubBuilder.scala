package zio.temporal.workflow

import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.ChildWorkflowCancellationType
import io.temporal.workflow.ChildWorkflowOptions
import io.temporal.workflow.Workflow
import zio.*
import zio.temporal.internal.ClassTagUtils
import zio.temporal.{ZRetryOptions, ZSearchAttribute}
import scala.jdk.CollectionConverters.*
import scala.reflect.ClassTag

object ZChildWorkflowStubBuilder {
  type Of[A]   = ZChildWorkflowStubBuilder[ZChildWorkflowStub.Of[A]]
  type Untyped = ZChildWorkflowStubBuilder[ZChildWorkflowStub.Untyped]

  private[temporal] def buildTyped[A: ClassTag]: ChildWorkflowOptions => ZChildWorkflowStub.Of[A] =
    options =>
      ZChildWorkflowStub.Of(
        new ZChildWorkflowStubImpl(
          Workflow.newUntypedChildWorkflowStub(
            ClassTagUtils.getWorkflowType[A],
            options
          ),
          ClassTagUtils.classOf[A]
        )
      )

  private[temporal] def buildUntyped(workflowType: String): ChildWorkflowOptions => ZChildWorkflowStub.Untyped =
    options =>
      new ZChildWorkflowStub.UntypedImpl(
        Workflow.newUntypedChildWorkflowStub(workflowType, options)
      )
}

class ZChildWorkflowStubBuilder[Res] private[zio] (
  buildImpl:         ChildWorkflowOptions => Res,
  additionalOptions: ChildWorkflowOptions.Builder => ChildWorkflowOptions.Builder) {

  def withNamespace(namespace: String): ZChildWorkflowStubBuilder[Res] =
    copy(_.setNamespace(namespace))

  def withWorkflowId(workflowId: String): ZChildWorkflowStubBuilder[Res] =
    copy(_.setWorkflowId(workflowId))

  def withWorkflowIdReusePolicy(policy: WorkflowIdReusePolicy): ZChildWorkflowStubBuilder[Res] =
    copy(_.setWorkflowIdReusePolicy(policy))

  def withWorkflowRunTimeout(timeout: Duration): ZChildWorkflowStubBuilder[Res] =
    copy(_.setWorkflowRunTimeout(timeout.asJava))

  def withWorkflowExecutionTimeout(timeout: Duration): ZChildWorkflowStubBuilder[Res] =
    copy(_.setWorkflowExecutionTimeout(timeout.asJava))

  def withWorkflowTaskTimeout(timeout: Duration): ZChildWorkflowStubBuilder[Res] =
    copy(_.setWorkflowTaskTimeout(timeout.asJava))

  def withTaskQueue(taskQueue: String): ZChildWorkflowStubBuilder[Res] =
    copy(_.setTaskQueue(taskQueue))

  def withRetryOptions(options: ZRetryOptions): ZChildWorkflowStubBuilder[Res] =
    copy(_.setRetryOptions(options.toJava))

  def withSearchAttributes(attrs: Map[String, ZSearchAttribute]): ZChildWorkflowStubBuilder[Res] =
    copy(_.setSearchAttributes(attrs))

  def withCronSchedule(schedule: String): ZChildWorkflowStubBuilder[Res] =
    copy(_.setCronSchedule(schedule))

  def withContextPropagators(propagators: Seq[ContextPropagator]): ZChildWorkflowStubBuilder[Res] =
    copy(_.setContextPropagators(propagators.asJava))

  def withCancellationType(cancellationType: ChildWorkflowCancellationType): ZChildWorkflowStubBuilder[Res] =
    copy(_.setCancellationType(cancellationType))

  /** Builds typed ZChildWorkflowStub
    * @return
    *   typed child workflow stub
    */
  def build: Res = {
    val options = additionalOptions(ChildWorkflowOptions.newBuilder()).build()
    buildImpl(options)
  }

  private def copy(
    options: ChildWorkflowOptions.Builder => ChildWorkflowOptions.Builder
  ): ZChildWorkflowStubBuilder[Res] =
    new ZChildWorkflowStubBuilder[Res](buildImpl, additionalOptions andThen options)

}
