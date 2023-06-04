package zio.temporal.activity

import zio._
import io.temporal.activity.ActivityCancellationType
import io.temporal.activity.ActivityOptions
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.Workflow
import zio.temporal.ZRetryOptions
import zio.temporal.internal.ClassTagUtils

import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

object ZActivityStubBuilderInitial {
  type Of[A]   = ZActivityStubBuilderInitial[ZActivityStub.Of[A]]
  type Untyped = ZActivityStubBuilderInitial[ZActivityStub.Untyped]

  private[temporal] def buildTyped[A: ClassTag]: ActivityOptions => ZActivityStub.Of[A] =
    options =>
      ZActivityStub.Of[A](
        new ZActivityStubImpl(
          Workflow.newUntypedActivityStub(options),
          ClassTagUtils.classOf[A]
        )
      )

  private[temporal] def buildUntyped: ActivityOptions => ZActivityStub.Untyped =
    options =>
      new ZActivityStub.UntypedImpl(
        Workflow.newUntypedActivityStub(options)
      )
}

final class ZActivityStubBuilderInitial[Res] private[zio] (buildImpl: ActivityOptions => Res) {

  /** Configures startToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setStartToCloseTimeout]]
    */
  def withStartToCloseTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    new ZActivityStubBuilder[Res](buildImpl, timeout, identity)
}

final class ZActivityStubBuilder[Res] private[zio] (
  buildImpl:           ActivityOptions => Res,
  startToCloseTimeout: Duration,
  additionalOptions:   ActivityOptions.Builder => ActivityOptions.Builder) {

  private def copy(options: ActivityOptions.Builder => ActivityOptions.Builder): ZActivityStubBuilder[Res] =
    new ZActivityStubBuilder[Res](buildImpl, startToCloseTimeout, additionalOptions andThen options)

  /** Configures scheduleToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToCloseTimeout]]
    */
  def withScheduleToCloseTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    copy(_.setScheduleToCloseTimeout(timeout.asJava))

  /** Configures scheduleToStartTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToStartTimeout]]
    */
  def withScheduleToStartTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    copy(_.setScheduleToStartTimeout(timeout.asJava))

  /** Configures heartbeatTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setHeartbeatTimeout]]
    */
  def withHeartbeatTimeout(timeout: Duration): ZActivityStubBuilder[Res] =
    copy(_.setHeartbeatTimeout(timeout.asJava))

  /** Configures taskQueue
    *
    * @see
    *   [[ActivityOptions.Builder.setTaskQueue]]
    */
  def withTaskQueue(taskQueue: String): ZActivityStubBuilder[Res] =
    copy(_.setTaskQueue(taskQueue))

  /** Configures retryOptions
    *
    * @see
    *   [[ActivityOptions.Builder.setRetryOptions]]
    * @see
    *   [[ZRetryOptions]]
    */
  def withRetryOptions(options: ZRetryOptions): ZActivityStubBuilder[Res] =
    copy(_.setRetryOptions(options.toJava))

  /** Configures contextPropagators
    *
    * @see
    *   [[ActivityOptions.Builder.setContextPropagators]]
    */
  def withContextPropagators(propagators: Seq[ContextPropagator]): ZActivityStubBuilder[Res] =
    copy(_.setContextPropagators(propagators.asJava))

  /** Configures cancellationType
    *
    * @see
    *   [[ActivityOptions.Builder.setCancellationType]]
    */
  def withCancellationType(cancellationType: ActivityCancellationType): ZActivityStubBuilder[Res] =
    copy(_.setCancellationType(cancellationType))

  /** Allows to specify options directly on the java SDK's [[ActivityOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ActivityOptions.Builder => ActivityOptions.Builder
  ): ZActivityStubBuilder[Res] = copy(f)

  /** Builds ActivityStub
    * @return
    *   activity stub
    */
  def build: Res = {
    val options = additionalOptions {
      ActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.asJava)
    }.build()

    buildImpl(options)
  }
}
