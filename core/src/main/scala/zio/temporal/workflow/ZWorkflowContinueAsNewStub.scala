package zio.temporal.workflow

import zio.Duration
import io.temporal.client.WorkflowStub
import io.temporal.workflow.ContinueAsNewOptions
import zio.temporal.{TemporalIO, ZWorkflowExecution, internalApi}
import zio.temporal.internal.{BasicStubOps, ClassTagUtils, Stubs, TemporalInteraction}
import zio.temporal.query.ZWorkflowStubQuerySyntax
import zio.temporal.signal.{ZWorkflowClientSignalWithStartSyntax, ZWorkflowStubSignalSyntax}

import java.util.concurrent.TimeUnit
import scala.reflect.ClassTag
import io.temporal.workflow.Workflow

sealed trait ZWorkflowContinueAsNewStub extends BasicStubOps {
  def workflowType: String
  def options: ContinueAsNewOptions
}

class ZWorkflowContinueAsNewStubImpl(
  val workflowType: String,
  val options:      ContinueAsNewOptions,
  val stubbedClass: Class[_])
    extends ZWorkflowContinueAsNewStub

object ZWorkflowContinueAsNewStub extends Stubs[ZWorkflowContinueAsNewStub] with ZWorkflowContinueAsNewStubSyntax {
  final implicit class Ops[A](private val self: ZWorkflowContinueAsNewStub.Of[A]) extends AnyVal {}
}

class ZWorkflowContinueAsNewStubBuilder[A: ClassTag: IsWorkflow](
  configure: ContinueAsNewOptions.Builder => ContinueAsNewOptions.Builder) {

  private def copy(
    more: ContinueAsNewOptions.Builder => ContinueAsNewOptions.Builder
  ): ZWorkflowContinueAsNewStubBuilder[A] =
    new ZWorkflowContinueAsNewStubBuilder[A](configure andThen more)

  def withWorkflowRunTimeout(timeout: Duration): ZWorkflowContinueAsNewStubBuilder[A] =
    copy(_.setWorkflowRunTimeout(timeout))

  def withTaskQueue(taskQueue: String): ZWorkflowContinueAsNewStubBuilder[A] =
    copy(_.setTaskQueue(taskQueue))

  def withWorkflowTaskTimeout(timeout: Duration): ZWorkflowContinueAsNewStubBuilder[A] =
    copy(_.setWorkflowTaskTimeout(timeout))

  def build: ZWorkflowContinueAsNewStub.Of[A] = {
    val options = configure(
      ContinueAsNewOptions.newBuilder()
    ).build()
    ZWorkflowContinueAsNewStub.Of[A](
      new ZWorkflowContinueAsNewStubImpl(
        ClassTagUtils.getWorkflowType[A],
        options,
        ClassTagUtils.classOf[A]
      )
    )
  }
}
