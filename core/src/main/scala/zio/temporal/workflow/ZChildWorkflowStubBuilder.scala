package zio.temporal.workflow

import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.ChildWorkflowCancellationType
import io.temporal.workflow.ChildWorkflowOptions
import io.temporal.api.enums.v1.ParentClosePolicy
import zio._
import zio.temporal.{ZRetryOptions, ZSearchAttribute, ZSearchAttributes}
import scala.jdk.CollectionConverters._

@deprecated("Build ZChildWorkflowOptions and provide it directly", since = "0.6.0")
object ZChildWorkflowStubBuilder {
  type Of[A]   = ZChildWorkflowStubBuilder[ZChildWorkflowStub.Of[A]]
  type Untyped = ZChildWorkflowStubBuilder[ZChildWorkflowStub.Untyped]
}

@deprecated("Build ZChildWorkflowOptions and provide it directly", since = "0.6.0")
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
    copy(_.setTypedSearchAttributes(ZSearchAttribute.toJavaSearchAttributes(attrs)))

  def withSearchAttributes(attrs: ZSearchAttributes): ZChildWorkflowStubBuilder[Res] =
    copy(_.setTypedSearchAttributes(attrs.toJava))

  def withCronSchedule(schedule: String): ZChildWorkflowStubBuilder[Res] =
    copy(_.setCronSchedule(schedule))

  def withContextPropagators(propagators: Seq[ContextPropagator]): ZChildWorkflowStubBuilder[Res] =
    copy(_.setContextPropagators(propagators.asJava))

  def withCancellationType(cancellationType: ChildWorkflowCancellationType): ZChildWorkflowStubBuilder[Res] =
    copy(_.setCancellationType(cancellationType))

  def withParentClosePolicy(policy: ParentClosePolicy): ZChildWorkflowStubBuilder[Res] =
    copy(_.setParentClosePolicy(policy))

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
