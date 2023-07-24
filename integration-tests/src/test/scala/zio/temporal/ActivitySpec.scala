package zio.temporal

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.test._
import java.util.concurrent.atomic.AtomicInteger

object ActivitySpec extends ZIOSpecDefault {
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    testEnvironment ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val spec = suite("ZActivity")(
    test("runs simple activity") {
      ZTestActivityEnvironment.activityOptions[Any].flatMap { implicit options =>
        for {
          _ <- ZTestActivityEnvironment.addActivityImplementation(new ZioActivityImpl)
          stub <- ZTestActivityEnvironment
                    .newActivityStub[ZioActivity]
                    .withStartToCloseTimeout(5.seconds)
                    .build
          result = stub.echo("hello")
        } yield assertTrue(result == "Echoed hello")
      }
    }.provideEnv,
    test("runs heartbeat activity from start") {
      ZTestActivityEnvironment.activityOptions[Any].flatMap { implicit options =>
        val numIterations = new AtomicInteger()
        for {
          _ <- ZTestActivityEnvironment.addActivityImplementation(new FibonacciHeartbeatActivityImpl(numIterations))
          stub <- ZTestActivityEnvironment
                    .newActivityStub[FibonacciHeartbeatActivity]
                    .withStartToCloseTimeout(1.minute)
                    .build
          result = stub.fibonacciSum(5)
        } yield {
          assertTrue(result == 12) &&
          assertTrue(numIterations.get() == 5)
        }
      }
    }.provideEnv,
    test("runs heartbeat activity from latest checkpoint") {
      ZTestActivityEnvironment.activityOptions[Any].flatMap { implicit options =>
        val numIterations = new AtomicInteger()
        for {
          _ <- ZTestActivityEnvironment.addActivityImplementation(new FibonacciHeartbeatActivityImpl(numIterations))
          stub <- ZTestActivityEnvironment
                    .newActivityStub[FibonacciHeartbeatActivity]
                    .withStartToCloseTimeout(1.minute)
                    .build
          _ <- ZTestActivityEnvironment.setHeartbeatDetails(
                 FibonacciHeartbeatActivityImpl.HeartbeatDetails(
                   sum = 4,
                   curr = 3,
                   prev = 2,
                   step = 3
                 )
               )
          result = stub.fibonacciSum(5)
        } yield {
          assertTrue(result == 12) &&
          assertTrue(numIterations.get() == 2)
        }
      }
    }.provideEnv
  ) @@ TestAspect.sequential @@ TestAspect.flaky

  private implicit class ProvidedTestkit[E, A](thunk: Spec[ZTestActivityEnvironment[Any] with Scope, E]) {
    def provideEnv: Spec[Scope, E] =
      thunk
        .provideSome[Scope](
          ZTestEnvironmentOptions.default,
          ZTestActivityEnvironment.make[Any]
        ) @@ TestAspect.withLiveClock @@ TestAspect.debug
  }
}
