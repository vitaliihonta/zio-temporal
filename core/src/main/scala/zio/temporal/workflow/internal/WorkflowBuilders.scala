package zio.temporal.workflow.internal

import io.temporal.client.{WorkflowClient, WorkflowOptions}
import scala.reflect.ClassTag
import zio._
import zio.temporal.internal.ClassTagUtils
import zio.temporal.workflow.{ZWorkflowStub, ZWorkflowStubImpl}

private[zio] object WorkflowBuilders {

  def typed[A: ClassTag](client: WorkflowClient): WorkflowOptions => UIO[ZWorkflowStub.Of[A]] =
    options =>
      ZIO.succeed {
        ZWorkflowStub.Of[A](
          new ZWorkflowStubImpl(
            client.newUntypedWorkflowStub(
              ClassTagUtils.getWorkflowType[A],
              options
            ),
            ClassTagUtils.classOf[A]
          )
        )
      }

  def untyped(
    workflowType: String,
    client:       WorkflowClient
  ): WorkflowOptions => UIO[ZWorkflowStub.Untyped] =
    options =>
      ZIO.succeed {
        new ZWorkflowStub.UntypedImpl(
          client.newUntypedWorkflowStub(workflowType, options)
        )
      }
}
