package zio.temporal.activity

import zio.*
import io.temporal.activity.LocalActivityOptions
import io.temporal.workflow.Workflow
import zio.temporal.ZRetryOptions
import scala.reflect.ClassTag

class ZLocalActivityStubBuilderInitial[A: ClassTag] private[zio] () {

  def withStartToCloseTimeout(timeout: Duration): ZLocalActivityStubBuilder[A] =
    new ZLocalActivityStubBuilder[A](timeout, identity)
}

class ZLocalActivityStubBuilder[A: ClassTag] private[zio] (
  startToCloseTimeout: Duration,
  additionalOptions:   LocalActivityOptions.Builder => LocalActivityOptions.Builder) {

  def withScheduleToCloseTimeout(timeout: Duration): ZLocalActivityStubBuilder[A] =
    copy(_.setScheduleToCloseTimeout(timeout.asJava))

  def withLocalRetryThreshold(localRetryThreshold: Duration): ZLocalActivityStubBuilder[A] =
    copy(_.setLocalRetryThreshold(localRetryThreshold.asJava))

  def withRetryOptions(options: ZRetryOptions): ZLocalActivityStubBuilder[A] =
    copy(_.setRetryOptions(options.toJava))

  /** Builds typed ZLocalActivityStub
    * @return
    *   typed local activity stub
    */
  def build: ZActivityStub.Of[A] = {
    val options = additionalOptions {
      LocalActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.asJava)
    }.build()

    ZActivityStub.Of[A](
      new ZActivityStubImpl(
        Workflow.newUntypedLocalActivityStub(options)
      )
    )
  }

  private def copy(
    options: LocalActivityOptions.Builder => LocalActivityOptions.Builder
  ): ZLocalActivityStubBuilder[A] =
    new ZLocalActivityStubBuilder[A](startToCloseTimeout, additionalOptions andThen options)

}

class ZLocalActivityUntypedStubBuilderInitial private[zio] () {

  def withStartToCloseTimeout(timeout: Duration): ZLocalActivityUntypedStubBuilder =
    new ZLocalActivityUntypedStubBuilder(timeout, identity)
}

class ZLocalActivityUntypedStubBuilder private[zio] (
  startToCloseTimeout: Duration,
  additionalOptions:   LocalActivityOptions.Builder => LocalActivityOptions.Builder) {

  def withScheduleToCloseTimeout(timeout: Duration): ZLocalActivityUntypedStubBuilder =
    copy(_.setScheduleToCloseTimeout(timeout.asJava))

  def withLocalRetryThreshold(localRetryThreshold: Duration): ZLocalActivityUntypedStubBuilder =
    copy(_.setLocalRetryThreshold(localRetryThreshold.asJava))

  def withRetryOptions(options: ZRetryOptions): ZLocalActivityUntypedStubBuilder =
    copy(_.setRetryOptions(options.toJava))

  /** Builds typed ZLocalActivityStub
    * @return
    *   typed local activity stub
    */
  def build: ZActivityStub.Untyped = {
    val options = additionalOptions {
      LocalActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.asJava)
    }.build()

    new ZActivityStub.UntypedImpl(
      Workflow.newUntypedLocalActivityStub(options)
    )
  }

  private def copy(
    options: LocalActivityOptions.Builder => LocalActivityOptions.Builder
  ): ZLocalActivityUntypedStubBuilder =
    new ZLocalActivityUntypedStubBuilder(startToCloseTimeout, additionalOptions andThen options)

}
