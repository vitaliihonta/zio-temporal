package zio.temporal.activity

import zio._
import io.temporal.activity.ActivityCancellationType
import io.temporal.activity.ActivityOptions
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.Workflow
import zio.temporal.ZRetryOptions
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

class ZActivityStubBuilderInitial[A: ClassTag] private[zio] () {

  /** Configures startToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setStartToCloseTimeout]]
    */
  def withStartToCloseTimeout(timeout: Duration): ZActivityStubBuilder[A] =
    new ZActivityStubBuilder[A](timeout, identity)
}

class ZActivityStubBuilder[A: ClassTag] private[zio] (
  startToCloseTimeout: Duration,
  additionalOptions:   ActivityOptions.Builder => ActivityOptions.Builder) {

  private def copy(options: ActivityOptions.Builder => ActivityOptions.Builder): ZActivityStubBuilder[A] =
    new ZActivityStubBuilder[A](startToCloseTimeout, additionalOptions andThen options)

  /** Configures scheduleToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToCloseTimeout]]
    */
  def withScheduleToCloseTimeout(timeout: Duration): ZActivityStubBuilder[A] =
    copy(_.setScheduleToCloseTimeout(timeout.asJava))

  /** Configures scheduleToStartTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToStartTimeout]]
    */
  def withScheduleToStartTimeout(timeout: Duration): ZActivityStubBuilder[A] =
    copy(_.setScheduleToStartTimeout(timeout.asJava))

  /** Configures heartbeatTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setHeartbeatTimeout]]
    */
  def withHeartbeatTimeout(timeout: Duration): ZActivityStubBuilder[A] =
    copy(_.setHeartbeatTimeout(timeout.asJava))

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

  /** Allows to specify options directly on the java SDK's [[ActivityOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ActivityOptions.Builder => ActivityOptions.Builder
  ): ZActivityStubBuilder[A] = copy(f)

  /** Builds ActivityStub
    * @return
    *   activity stub
    */
  def build: ZActivityStub.Of[A] = {
    val options = additionalOptions {
      ActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.asJava)
    }.build()

    ZActivityStub.Of[A](
      new ZActivityStubImpl(
        Workflow.newUntypedActivityStub(options)
      )
    )
  }
}

// untyped
class ZActivityStubUntypedBuilderInitial private[zio] () {

  /** Configures startToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setStartToCloseTimeout]]
    */
  def withStartToCloseTimeout(timeout: Duration): ZActivityUntypedStubBuilder =
    new ZActivityUntypedStubBuilder(timeout, identity)
}

class ZActivityUntypedStubBuilder private[zio] (
  startToCloseTimeout: Duration,
  additionalOptions:   ActivityOptions.Builder => ActivityOptions.Builder) {

  private def copy(options: ActivityOptions.Builder => ActivityOptions.Builder): ZActivityUntypedStubBuilder =
    new ZActivityUntypedStubBuilder(startToCloseTimeout, additionalOptions andThen options)

  /** Configures scheduleToCloseTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToCloseTimeout]]
    */
  def withScheduleToCloseTimeout(timeout: Duration): ZActivityUntypedStubBuilder =
    copy(_.setScheduleToCloseTimeout(timeout.asJava))

  /** Configures scheduleToStartTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setScheduleToStartTimeout]]
    */
  def withScheduleToStartTimeout(timeout: Duration): ZActivityUntypedStubBuilder =
    copy(_.setScheduleToStartTimeout(timeout.asJava))

  /** Configures heartbeatTimeout
    *
    * @see
    *   [[ActivityOptions.Builder.setHeartbeatTimeout]]
    */
  def withHeartbeatTimeout(timeout: Duration): ZActivityUntypedStubBuilder =
    copy(_.setHeartbeatTimeout(timeout.asJava))

  /** Configures taskQueue
    *
    * @see
    *   [[ActivityOptions.Builder.setTaskQueue]]
    */
  def withTaskQueue(taskQueue: String): ZActivityUntypedStubBuilder =
    copy(_.setTaskQueue(taskQueue))

  /** Configures retryOptions
    *
    * @see
    *   [[ActivityOptions.Builder.setRetryOptions]]
    * @see
    *   [[ZRetryOptions]]
    */
  def withRetryOptions(options: ZRetryOptions): ZActivityUntypedStubBuilder =
    copy(_.setRetryOptions(options.toJava))

  /** Configures contextPropagators
    *
    * @see
    *   [[ActivityOptions.Builder.setContextPropagators]]
    */
  def withContextPropagators(propagators: Seq[ContextPropagator]): ZActivityUntypedStubBuilder =
    copy(_.setContextPropagators(propagators.asJava))

  /** Configures cancellationType
    *
    * @see
    *   [[ActivityOptions.Builder.setCancellationType]]
    */
  def withCancellationType(cancellationType: ActivityCancellationType): ZActivityUntypedStubBuilder =
    copy(_.setCancellationType(cancellationType))

  /** Allows to specify options directly on the java SDK's [[ActivityOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ActivityOptions.Builder => ActivityOptions.Builder
  ): ZActivityUntypedStubBuilder = copy(f)

  /** Builds ActivityStub
    * @return
    *   activity stub
    */
  def build: ZActivityStub.Untyped = {
    val options = additionalOptions {
      ActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.asJava)
    }.build()

    new ZActivityStub.UntypedImpl(
      Workflow.newUntypedActivityStub(options)
    )
  }
}
