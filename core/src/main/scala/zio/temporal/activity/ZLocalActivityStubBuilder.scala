package zio.temporal.activity

import zio._
import io.temporal.activity.LocalActivityOptions
import zio.temporal.ZRetryOptions

@deprecated("Build ZLocalActivityOptions and provide it directly", since = "0.6.0")
object ZLocalActivityStubBuilderInitial {
  type Of[A]   = ZLocalActivityStubBuilderInitial[ZActivityStub.Of[A]]
  type Untyped = ZLocalActivityStubBuilderInitial[ZActivityStub.Untyped]
}

@deprecated("Build ZLocalActivityOptions and provide it directly", since = "0.6.0")
class ZLocalActivityStubBuilderInitial[Res] private[zio] (buildImpl: LocalActivityOptions => Res) {

  def withStartToCloseTimeout(timeout: Duration): ZLocalActivityStubBuilder[Res] =
    new ZLocalActivityStubBuilder[Res](buildImpl, timeout, identity)
}

@deprecated("Build ZLocalActivityOptions and provide it directly", since = "0.6.0")
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
