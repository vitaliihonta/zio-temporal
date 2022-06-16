package ztemporal.activity

import io.temporal.activity.LocalActivityOptions
import io.temporal.workflow.Workflow
import ztemporal.ZRetryOptions
import ztemporal.internal.ClassTagUtils

import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

class ZLocalActivityStubBuilderInitial[A] private[ztemporal] (private val ctg: ClassTag[A]) extends AnyVal {

  def withStartToCloseTimeout(timeout: FiniteDuration): ZLocalActivityStubBuilder[A] =
    new ZLocalActivityStubBuilder[A](timeout, identity)(ctg)
}

class ZLocalActivityStubBuilder[A] private[ztemporal] (
  startToCloseTimeout: FiniteDuration,
  additionalOptions:   LocalActivityOptions.Builder => LocalActivityOptions.Builder
)(implicit ctg:        ClassTag[A]) {

  def withScheduleToCloseTimeout(timeout: FiniteDuration): ZLocalActivityStubBuilder[A] =
    copy(_.setScheduleToCloseTimeout(timeout.toJava))

  def withLocalRetryThreshold(localRetryThreshold: FiniteDuration): ZLocalActivityStubBuilder[A] =
    copy(_.setLocalRetryThreshold(localRetryThreshold.toJava))

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
        .setStartToCloseTimeout(startToCloseTimeout.toJava)
    }.build()

    Workflow.newLocalActivityStub[A](ClassTagUtils.classOf[A], options)
  }

  private def copy(
    options: LocalActivityOptions.Builder => LocalActivityOptions.Builder
  ): ZLocalActivityStubBuilder[A] =
    new ZLocalActivityStubBuilder[A](startToCloseTimeout, additionalOptions andThen options)

}
