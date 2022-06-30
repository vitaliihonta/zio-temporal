package zio.temporal.activity

import zio._
import io.temporal.activity.LocalActivityOptions
import io.temporal.workflow.Workflow
import zio.temporal.ZRetryOptions
import zio.temporal.internal.ClassTagUtils
import scala.reflect.ClassTag

class ZLocalActivityStubBuilderInitial[A] private[zio] (private val ctg: ClassTag[A]) extends AnyVal {

  def withStartToCloseTimeout(timeout: Duration): ZLocalActivityStubBuilder[A] =
    new ZLocalActivityStubBuilder[A](timeout, identity)(ctg)
}

class ZLocalActivityStubBuilder[A] private[zio] (
  startToCloseTimeout: Duration,
  additionalOptions:   LocalActivityOptions.Builder => LocalActivityOptions.Builder
)(implicit ctg:        ClassTag[A]) {

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
  def build: A = {
    val options = additionalOptions {
      LocalActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.asJava)
    }.build()

    Workflow.newLocalActivityStub[A](ClassTagUtils.classOf[A], options)
  }

  private def copy(
    options: LocalActivityOptions.Builder => LocalActivityOptions.Builder
  ): ZLocalActivityStubBuilder[A] =
    new ZLocalActivityStubBuilder[A](startToCloseTimeout, additionalOptions andThen options)

}
