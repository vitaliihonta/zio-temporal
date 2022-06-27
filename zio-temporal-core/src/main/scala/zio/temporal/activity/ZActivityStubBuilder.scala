package zio.temporal.activity

import io.temporal.activity.{ActivityCancellationType, ActivityOptions}
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.Workflow
import zio.temporal.ZRetryOptions
import zio.temporal.internal.ClassTagUtils

import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

class ZActivityStubBuilderInitial[A] private[zio] (private val ctg: ClassTag[A]) extends AnyVal {

  /** Configures startToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setStartToCloseTimeout]]
    */
  def withStartToCloseTimeout(timeout: FiniteDuration): ZActivityStubBuilder[A] =
    new ZActivityStubBuilder[A](timeout, identity)(ctg)
}

class ZActivityStubBuilder[A] private[zio] (
  startToCloseTimeout: FiniteDuration,
  additionalOptions:   ActivityOptions.Builder => ActivityOptions.Builder
)(implicit ctg:        ClassTag[A]) {

  private def copy(options: ActivityOptions.Builder => ActivityOptions.Builder): ZActivityStubBuilder[A] =
    new ZActivityStubBuilder[A](startToCloseTimeout, additionalOptions andThen options)

  /** Configures scheduleToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToCloseTimeout]]
    */
  def withScheduleToCloseTimeout(timeout: FiniteDuration): ZActivityStubBuilder[A] =
    copy(_.setScheduleToCloseTimeout(timeout.toJava))

  /** Configures scheduleToStartTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToStartTimeout]]
    */
  def withScheduleToStartTimeout(timeout: FiniteDuration): ZActivityStubBuilder[A] =
    copy(_.setScheduleToStartTimeout(timeout.toJava))

  /** Configures heartbeatTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setHeartbeatTimeout]]
    */
  def withHeartbeatTimeout(timeout: FiniteDuration): ZActivityStubBuilder[A] =
    copy(_.setHeartbeatTimeout(timeout.toJava))

  /** Configures taskQueue
    *
    * @see
    *   [[ActivityOptions.Builder.setTaskQueue]]
    */
  def withTaskQueue(taskQueue: String): ZActivityStubBuilder[A] =
    copy(_.setTaskQueue(taskQueue))

  /** Configures retryOptions
    *
    * @see
    *   [[ActivityOptions.Builder.setRetryOptions]]
    * @see
    *   [[ZRetryOptions]]
    */
  def withRetryOptions(options: ZRetryOptions): ZActivityStubBuilder[A] =
    copy(_.setRetryOptions(options.toJava))

  /** Configures contextPropagators
    *
    * @see
    *   [[ActivityOptions.Builder.setContextPropagators]]
    */
  def withContextPropagators(propagators: Seq[ContextPropagator]): ZActivityStubBuilder[A] =
    copy(_.setContextPropagators(propagators.asJava))

  /** Configures cancellationType
    *
    * @see
    *   [[ActivityOptions.Builder.setCancellationType]]
    */
  def withCancellationType(cancellationType: ActivityCancellationType): ZActivityStubBuilder[A] =
    copy(_.setCancellationType(cancellationType))

  /** Builds ActivityStub
    * @return
    *   activity stub
    */
  def build: A = {
    val options = additionalOptions {
      ActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.toJava)
    }.build()

    Workflow.newActivityStub[A](ClassTagUtils.classOf[A], options)
  }
}
