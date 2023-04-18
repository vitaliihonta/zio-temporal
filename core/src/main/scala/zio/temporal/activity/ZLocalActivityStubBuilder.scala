package zio.temporal.activity

import zio.*
import io.temporal.activity.LocalActivityOptions
import io.temporal.workflow.Workflow
import zio.temporal.ZRetryOptions
import scala.reflect.ClassTag

object ZLocalActivityStubBuilderInitial {
  type Of[A]   = ZLocalActivityStubBuilderInitial[ZActivityStub.Of[A]]
  type Untyped = ZLocalActivityStubBuilderInitial[ZActivityStub.Untyped]

  private[temporal] def buildTyped[A: ClassTag]: LocalActivityOptions => ZActivityStub.Of[A] =
    options =>
      ZActivityStub.Of[A](
        new ZActivityStubImpl(
          Workflow.newUntypedLocalActivityStub(options)
        )
      )

  private[temporal] def buildUntyped: LocalActivityOptions => ZActivityStub.Untyped =
    options =>
      new ZActivityStub.UntypedImpl(
        Workflow.newUntypedLocalActivityStub(options)
      )
}

class ZLocalActivityStubBuilderInitial[Res] private[zio] (buildImpl: LocalActivityOptions => Res) {

  def withStartToCloseTimeout(timeout: Duration): ZLocalActivityStubBuilder[Res] =
    new ZLocalActivityStubBuilder[Res](buildImpl, timeout, identity)
}

class ZLocalActivityStubBuilder[Res] private[zio] (
  buildImpl:           LocalActivityOptions => Res,
  startToCloseTimeout: Duration,
  additionalOptions:   LocalActivityOptions.Builder => LocalActivityOptions.Builder) {

  def withScheduleToCloseTimeout(timeout: Duration): ZLocalActivityStubBuilder[Res] =
    copy(_.setScheduleToCloseTimeout(timeout.asJava))

  def withLocalRetryThreshold(localRetryThreshold: Duration): ZLocalActivityStubBuilder[Res] =
    copy(_.setLocalRetryThreshold(localRetryThreshold.asJava))

  def withRetryOptions(options: ZRetryOptions): ZLocalActivityStubBuilder[Res] =
    copy(_.setRetryOptions(options.toJava))

  /** Builds typed ZLocalActivityStub
    * @return
    *   typed local activity stub
    */
  def build: Res = {
    val options = additionalOptions {
      LocalActivityOptions
        .newBuilder()
        .setStartToCloseTimeout(startToCloseTimeout.asJava)
    }.build()

    buildImpl(options)
  }

  private def copy(
    options: LocalActivityOptions.Builder => LocalActivityOptions.Builder
  ): ZLocalActivityStubBuilder[Res] =
    new ZLocalActivityStubBuilder[Res](buildImpl, startToCloseTimeout, additionalOptions andThen options)

}
