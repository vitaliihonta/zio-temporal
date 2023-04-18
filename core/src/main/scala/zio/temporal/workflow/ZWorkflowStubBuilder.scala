package zio.temporal.workflow

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import zio.*
import zio.temporal.{ZRetryOptions, ZSearchAttribute, simpleNameOf}
import scala.reflect.ClassTag

object ZWorkflowStubBuilderTaskQueueDsl {
  type Of[A]   = ZWorkflowStubBuilderTaskQueueDsl[ZWorkflowStub.Of[A]]
  type Untyped = ZWorkflowStubBuilderTaskQueueDsl[ZWorkflowStub.Untyped]

  private[temporal] def typed[A: ClassTag]: (WorkflowClient, WorkflowOptions) => ZWorkflowStub.Of[A] =
    (client, options) =>
      ZWorkflowStub.Of(
        new ZWorkflowStubImpl(
          client.newUntypedWorkflowStub(
            simpleNameOf[A],
            options
          )
        )
      )

  private[temporal] def untyped(workflowType: String): (WorkflowClient, WorkflowOptions) => ZWorkflowStub.Untyped =
    (client, options) =>
      new ZWorkflowStub.UntypedImpl(
        client.newUntypedWorkflowStub(workflowType, options)
      )
}

final class ZWorkflowStubBuilderTaskQueueDsl[Res] private[zio] (
  client:    WorkflowClient,
  buildImpl: (WorkflowClient, WorkflowOptions) => Res) {

  def withTaskQueue(taskQueue: String): ZWorkflowStubBuilderWorkflowIdDsl[Res] =
    new ZWorkflowStubBuilderWorkflowIdDsl[Res](client, buildImpl, taskQueue)
}

final class ZWorkflowStubBuilderWorkflowIdDsl[Res] private[zio] (
  client:    WorkflowClient,
  buildImpl: (WorkflowClient, WorkflowOptions) => Res,
  taskQueue: String) {

  def withWorkflowId(workflowId: String): ZWorkflowStubBuilder[Res] =
    new ZWorkflowStubBuilder[Res](client, buildImpl, taskQueue, workflowId, additionalConfig = identity)
}

final class ZWorkflowStubBuilder[Res] private[zio] (
  client:           WorkflowClient,
  buildImpl:        (WorkflowClient, WorkflowOptions) => Res,
  taskQueue:        String,
  workflowId:       String,
  additionalConfig: WorkflowOptions.Builder => WorkflowOptions.Builder) {

  private def copy(config: WorkflowOptions.Builder => WorkflowOptions.Builder): ZWorkflowStubBuilder[Res] =
    new ZWorkflowStubBuilder[Res](client, buildImpl, taskQueue, workflowId, additionalConfig andThen config)

  def withSearchAttributes(attrs: Map[String, ZSearchAttribute]): ZWorkflowStubBuilder[Res] =
    copy(_.setSearchAttributes(attrs))

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
  def build: UIO[Res] =
    ZIO.succeed {
      val options =
        additionalConfig {
          WorkflowOptions
            .newBuilder()
            .setTaskQueue(taskQueue)
            .setWorkflowId(workflowId)
        }.build()

      buildImpl(client, options)
    }
}
