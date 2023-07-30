package zio.temporal

import zio._
import zio.temporal.testkit.{ZTestActivityEnvironment, ZTestEnvironmentOptions, ZTestWorkflowEnvironment}
import zio.test._

abstract class BaseTemporalSpec extends ZIOSpecDefault {

  protected implicit class ProvideWorkflowEnvironmentOps[E, A](
    thunk: Spec[ZTestWorkflowEnvironment[Any] with Scope, E]) {
    def provideTestWorkflowEnv: Spec[Scope, E] =
      thunk.provideSome[Scope](ZTestWorkflowEnvironment.makeDefault[Any]) @@ TestAspect.withLiveClock
  }

  protected implicit class ProvideActivityEnvironmentOps[E, A](
    thunk: Spec[ZTestActivityEnvironment[Any] with Scope, E]) {
    def provideTestActivityEnv: Spec[Scope, E] =
      thunk.provideSome[Scope](ZTestActivityEnvironment.makeDefault[Any]) @@ TestAspect.withLiveClock
  }
}
