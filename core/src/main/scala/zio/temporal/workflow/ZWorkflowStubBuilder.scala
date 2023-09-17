package zio.temporal.workflow

import io.temporal.client.WorkflowOptions
import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.common.context.ContextPropagator
import zio._
import zio.temporal.{ZRetryOptions, ZSearchAttribute, ZSearchAttributes}
import scala.jdk.CollectionConverters._

@deprecated("Build ZWorkflowOptions and provide it directly", since = "0.5.0")
object ZWorkflowStubBuilderTaskQueueDsl {
  type Of[A]   = ZWorkflowStubBuilderTaskQueueDsl[UIO[ZWorkflowStub.Of[A]]]
  type Untyped = ZWorkflowStubBuilderTaskQueueDsl[UIO[ZWorkflowStub.Untyped]]
}

@deprecated("Build ZWorkflowOptions and provide it directly", since = "0.5.0")
final class ZWorkflowStubBuilderTaskQueueDsl[Res] private[zio] (
  buildImpl: WorkflowOptions => Res) {

  def withTaskQueue(taskQueue: String): ZWorkflowStubBuilderWorkflowIdDsl[Res] =
    new ZWorkflowStubBuilderWorkflowIdDsl[Res](buildImpl, taskQueue)
}

@deprecated("Build ZWorkflowOptions and provide it directly", since = "0.5.0")
final class ZWorkflowStubBuilderWorkflowIdDsl[Res] private[zio] (
  buildImpl: WorkflowOptions => Res,
  taskQueue: String) {

  def withWorkflowId(workflowId: String): ZWorkflowStubBuilder[Res] =
    new ZWorkflowStubBuilder[Res](buildImpl, taskQueue, workflowId, additionalConfig = identity)
}

@deprecated("Build ZWorkflowOptions and provide it directly", since = "0.5.0")
final class ZWorkflowStubBuilder[Res] private[zio] (
  buildImpl:        WorkflowOptions => Res,
  taskQueue:        String,
  workflowId:       String,
  additionalConfig: WorkflowOptions.Builder => WorkflowOptions.Builder) {

  private def copy(config: WorkflowOptions.Builder => WorkflowOptions.Builder): ZWorkflowStubBuilder[Res] =
    new ZWorkflowStubBuilder[Res](buildImpl, taskQueue, workflowId, additionalConfig andThen config)

  def withSearchAttributes(attrs: Map[String, ZSearchAttribute]): ZWorkflowStubBuilder[Res] =
    copy(_.setTypedSearchAttributes(ZSearchAttribute.toJavaSearchAttributes(attrs)))

  def withSearchAttributes(attrs: ZSearchAttributes): ZWorkflowStubBuilder[Res] =
    copy(_.setTypedSearchAttributes(attrs.toJava))

  def withCronSchedule(schedule: String): ZWorkflowStubBuilder[Res] =
    copy(_.setCronSchedule(schedule))

  def withWorkflowRunTimeout(timeout: Duration): ZWorkflowStubBuilder[Res] =
    copy(_.setWorkflowRunTimeout(timeout.asJava))

  def withWorkflowTaskTimeout(timeout: Duration): ZWorkflowStubBuilder[Res] =
    copy(_.setWorkflowTaskTimeout(timeout.asJava))

  def withWorkflowExecutionTimeout(timeout: Duration): ZWorkflowStubBuilder[Res] =
    copy(_.setWorkflowExecutionTimeout(timeout.asJava))

  def withRetryOptions(options: ZRetryOptions): ZWorkflowStubBuilder[Res] =
    copy(_.setRetryOptions(options.toJava))

  def withWorkflowIdReusePolicy(value: WorkflowIdReusePolicy): ZWorkflowStubBuilder[Res] =
    copy(_.setWorkflowIdReusePolicy(value))

  def withMemo(values: (String, AnyRef)*): ZWorkflowStubBuilder[Res] =
    copy(_.setMemo(values.toMap.asJava))

  def withContextPropagators(values: ContextPropagator*): ZWorkflowStubBuilder[Res] =
    copy(_.setContextPropagators(values.asJava))

  /** Allows to specify options directly on the java SDK's [[WorkflowOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkflowOptions.Builder => WorkflowOptions.Builder
  ): ZWorkflowStubBuilder[Res] = copy(f)

  /** Builds typed ZWorkflowStub
    * @return
    *   typed workflow stub
    */
  def build: Res = {
    val options =
      additionalConfig {
        WorkflowOptions
          .newBuilder()
          .setTaskQueue(taskQueue)
          .setWorkflowId(workflowId)
      }.build()

    buildImpl(options)
  }
}
