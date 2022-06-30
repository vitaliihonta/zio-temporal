package zio.temporal.workflow

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import zio._
import zio.temporal.ZRetryOptions
import zio.temporal.ZSearchAttribute
import scala.compat.java8.DurationConverters._
import scala.reflect.ClassTag

final class ZWorkflowStubBuilderTaskQueueDsl[A] private[zio] (client: WorkflowClient, ctg: ClassTag[A]) {

  def withTaskQueue(taskQueue: String): ZWorkflowStubBuilderWorkflowIdDsl[A] =
    new ZWorkflowStubBuilderWorkflowIdDsl[A](client, ctg, taskQueue)
}

final class ZWorkflowStubBuilderWorkflowIdDsl[A] private[zio] (
  client:    WorkflowClient,
  ctg:       ClassTag[A],
  taskQueue: String) {

  def withWorkflowId(workflowId: String): ZWorkflowStubBuilder[A] =
    new ZWorkflowStubBuilder[A](client, ctg, taskQueue, workflowId, additionalConfig = identity)
}

final class ZWorkflowStubBuilder[A] private[zio] (
  client:           WorkflowClient,
  ctg:              ClassTag[A],
  taskQueue:        String,
  workflowId:       String,
  additionalConfig: WorkflowOptions.Builder => WorkflowOptions.Builder) {

  private def copy(config: WorkflowOptions.Builder => WorkflowOptions.Builder): ZWorkflowStubBuilder[A] =
    new ZWorkflowStubBuilder[A](client, ctg, taskQueue, workflowId, additionalConfig andThen config)

  def withSearchAttributes(attrs: Map[String, ZSearchAttribute]): ZWorkflowStubBuilder[A] =
    copy(_.setSearchAttributes(attrs))

  def withCronSchedule(schedule: String): ZWorkflowStubBuilder[A] =
    copy(_.setCronSchedule(schedule))

  def withWorkflowRunTimeout(timeout: Duration): ZWorkflowStubBuilder[A] =
    copy(_.setWorkflowRunTimeout(timeout.asJava))

  def withWorkflowTaskTimeout(timeout: Duration): ZWorkflowStubBuilder[A] =
    copy(_.setWorkflowTaskTimeout(timeout.asJava))

  def withRetryOptions(options: ZRetryOptions): ZWorkflowStubBuilder[A] =
    copy(_.setRetryOptions(options.toJava))

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
        client.newWorkflowStub(
          ctg.runtimeClass.asInstanceOf[Class[A]],
          options
        )
      )
    }
}
