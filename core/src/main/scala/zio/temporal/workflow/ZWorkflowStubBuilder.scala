package zio.temporal.workflow

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import zio.*
import zio.temporal.{ZRetryOptions, ZSearchAttribute, simpleNameOf}
import scala.reflect.ClassTag

final class ZWorkflowStubBuilderTaskQueueDsl[A: ClassTag] private[zio] (client: WorkflowClient) {

  def withTaskQueue(taskQueue: String): ZWorkflowStubBuilderWorkflowIdDsl[A] =
    new ZWorkflowStubBuilderWorkflowIdDsl[A](client, taskQueue)
}

final class ZWorkflowStubBuilderWorkflowIdDsl[A: ClassTag] private[zio] (
  client:    WorkflowClient,
  taskQueue: String) {

  def withWorkflowId(workflowId: String): ZWorkflowStubBuilder[A] =
    new ZWorkflowStubBuilder[A](client, taskQueue, workflowId, additionalConfig = identity)
}

final class ZWorkflowStubBuilder[A: ClassTag] private[zio] (
  client:           WorkflowClient,
  taskQueue:        String,
  workflowId:       String,
  additionalConfig: WorkflowOptions.Builder => WorkflowOptions.Builder) {

  private def copy(config: WorkflowOptions.Builder => WorkflowOptions.Builder): ZWorkflowStubBuilder[A] =
    new ZWorkflowStubBuilder[A](client, taskQueue, workflowId, additionalConfig andThen config)

  def withSearchAttributes(attrs: Map[String, ZSearchAttribute]): ZWorkflowStubBuilder[A] =
    copy(_.setSearchAttributes(attrs))

  def withCronSchedule(schedule: String): ZWorkflowStubBuilder[A] =
    copy(_.setCronSchedule(schedule))

  def withWorkflowRunTimeout(timeout: Duration): ZWorkflowStubBuilder[A] =
    copy(_.setWorkflowRunTimeout(timeout.asJava))

  def withWorkflowTaskTimeout(timeout: Duration): ZWorkflowStubBuilder[A] =
    copy(_.setWorkflowTaskTimeout(timeout.asJava))

  def withWorkflowExecutionTimeout(timeout: Duration): ZWorkflowStubBuilder[A] =
    copy(_.setWorkflowExecutionTimeout(timeout.asJava))

  def withRetryOptions(options: ZRetryOptions): ZWorkflowStubBuilder[A] =
    copy(_.setRetryOptions(options.toJava))

  /** Allows to specify options directly on the java SDK's [[WorkflowOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkflowOptions.Builder => WorkflowOptions.Builder
  ): ZWorkflowStubBuilder[A] = copy(f)

  /** Builds typed ZWorkflowStub
    * @return
    *   typed workflow stub
    */
  def build: UIO[ZWorkflowStub.Of[A]] =
    ZIO.succeed {
      val options =
        additionalConfig {
          WorkflowOptions
            .newBuilder()
            .setTaskQueue(taskQueue)
            .setWorkflowId(workflowId)
        }.build()

      ZWorkflowStub.Of(
        new ZWorkflowStubImpl(
          client.newUntypedWorkflowStub(
            simpleNameOf[A],
            options
          )
        )
      )
    }
}
