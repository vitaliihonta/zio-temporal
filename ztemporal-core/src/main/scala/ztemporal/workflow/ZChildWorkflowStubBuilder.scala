package ztemporal.workflow

import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.ChildWorkflowCancellationType
import io.temporal.workflow.ChildWorkflowOptions
import io.temporal.workflow.Workflow
import ztemporal.ZRetryOptions
import ztemporal.ZSearchAttribute
import ztemporal.internal.ClassTagUtils

import scala.compat.java8.DurationConverters._
import scala.jdk.CollectionConverters._
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

class ZChildWorkflowStubBuilder[A: ClassTag] private[ztemporal] (
  additionalOptions: ChildWorkflowOptions.Builder => ChildWorkflowOptions.Builder) {

  def withNamespace(namespace: String): ZChildWorkflowStubBuilder[A] =
    copy(_.setNamespace(namespace))

  def withWorkflowId(workflowId: String): ZChildWorkflowStubBuilder[A] =
    copy(_.setWorkflowId(workflowId))

  def withWorkflowIdReusePolicy(policy: WorkflowIdReusePolicy): ZChildWorkflowStubBuilder[A] =
    copy(_.setWorkflowIdReusePolicy(policy))

  def withWorkflowRunTimeout(timeout: FiniteDuration): ZChildWorkflowStubBuilder[A] =
    copy(_.setWorkflowRunTimeout(timeout.toJava))

  def withWorkflowExecutionTimeout(timeout: FiniteDuration): ZChildWorkflowStubBuilder[A] =
    copy(_.setWorkflowExecutionTimeout(timeout.toJava))

  def withWorkflowTaskTimeout(timeout: FiniteDuration): ZChildWorkflowStubBuilder[A] =
    copy(_.setWorkflowTaskTimeout(timeout.toJava))

  def withTaskQueue(taskQueue: String): ZChildWorkflowStubBuilder[A] =
    copy(_.setTaskQueue(taskQueue))

  def withRetryOptions(options: ZRetryOptions): ZChildWorkflowStubBuilder[A] =
    copy(_.setRetryOptions(options.toJava))

  def withSearchAttributes(attrs: Map[String, ZSearchAttribute]): ZChildWorkflowStubBuilder[A] =
    copy(_.setSearchAttributes(attrs))

  def withCronSchedule(schedule: String): ZChildWorkflowStubBuilder[A] =
    copy(_.setCronSchedule(schedule))

  def withContextPropagators(propagators: Seq[ContextPropagator]): ZChildWorkflowStubBuilder[A] =
    copy(_.setContextPropagators(propagators.asJava))

  def withCancellationType(cancellationType: ChildWorkflowCancellationType): ZChildWorkflowStubBuilder[A] =
    copy(_.setCancellationType(cancellationType))

  /** Builds typed ZChildWorkflowStub
    * @return
    *   typed child workflow stub
    */
  def build: ZChildWorkflowStub.Of[A] = {
    val options = additionalOptions(ChildWorkflowOptions.newBuilder()).build()
    ZChildWorkflowStub.Of(
      Workflow.newChildWorkflowStub(ClassTagUtils.classOf[A], options)
    )
  }

  private def copy(
    options: ChildWorkflowOptions.Builder => ChildWorkflowOptions.Builder
  ): ZChildWorkflowStubBuilder[A] =
    new ZChildWorkflowStubBuilder[A](additionalOptions andThen options)

}
