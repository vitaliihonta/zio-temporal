# Activities
Temporal provides with `ZTestActivityEnvironment` that allows to run activities in a local test environment.  
General business logic, as well Temporal functionality (such as `Activity heartbeats`) can be tested locally with the testkit.  

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.test._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.testkit._
```

## Simple tests
Imagine the following Activity interface:

```scala mdoc:silent
@activityInterface
trait EchoActivities {
  def echo(what: String): String
}

class EchoActivitiesImpl(implicit options: ZActivityOptions[Any]) extends EchoActivities {
  override def echo(what: String): String =
    ZActivity.run {
      ZIO
        .log(s"Echo message=$what")
        .as(s"Echoed $what")
    }
}
```

Here is an example of testing this activity:

```scala mdoc:silent
object EchoActivitySpec extends ZIOSpecDefault {
  override val spec = suite("EchoActivities")(
    test("echoes message") {
      ZTestActivityEnvironment.activityOptions[Any].flatMap { implicit options =>
        for {
          // Provide a "factory" method to construct the activity
          _ <- ZTestActivityEnvironment.addActivityImplementation(new EchoActivitiesImpl)
          // Get the activity stub
          stub <- ZTestActivityEnvironment
            .newActivityStub[EchoActivities]
            .withStartToCloseTimeout(5.seconds)
            .build
          // Invoke the activity
          result = stub.echo("hello")
          // Test your code
        } yield assertTrue(result == "Echoed hello")
      }
    }
  ).provide(
    ZTestEnvironmentOptions.default,
    ZTestActivityEnvironment.make[Any]
  ) @@ TestAspect.withLiveClock
}
```

**Notes**
- `ZTestActivityEnvironment` companion object has a plenty of methods to configure the test environment
  - `ZTestActivityEnvironment.activityOptions[R]` provides with `ZActivityOptions` needed to run ZIO inside activities
  - `ZTestActivityEnvironment.addActivityImplementation` populates the test environment with Activity implementations
  - `ZTestActivityEnvironment.newActivityStub` returns a stub  for testing
- Unlike real Activity stubs, those returned by `ZTestActivityEnvironment` run locally. Therefore, its methods can be called directly (without wrapping into `ZActivityStub.execute`)
- It's recommended to use `TestAspect.withLiveClock` in your tests to avoid hanging tests
  - It may happen because both zio-test & temporal-testkit use their own virtual clock

## Testing heartbeats
Activities using heartbeats might be tested as well. `ZTestActivityEnvironment` allows setting heartbeat details before running the activity.  
With this feature, it's possible to check whether the Activity successfully recovers from the previous "checkpoint":

Imagine the following activity:

```scala mdoc:silent
import io.temporal.client.ActivityCompletionException
import java.util.concurrent.atomic.AtomicInteger

@activityInterface
trait FibonacciHeartbeatActivity {
  // Let's assume that calculating fibonacci numbers takes some time...
  def fibonacciSum(n: Int): BigDecimal
}

object FibonacciHeartbeatActivityImpl {
  case class HeartbeatDetails(sum: BigDecimal, curr: Int, prev: Int, step: Int)
}

// The counter is needed for demo purposes
class FibonacciHeartbeatActivityImpl(iterationsCounter: AtomicInteger)(
  implicit options: ZActivityOptions[Any]
) extends FibonacciHeartbeatActivity {
  
  import FibonacciHeartbeatActivityImpl.HeartbeatDetails

  override def fibonacciSum(n: Int): BigDecimal =
    ZActivity.run {
      val context = ZActivity.executionContext

      // Iteration loop
      def iter(
        sum: BigDecimal, 
        curr: Int, // current fibonacci number 
        prev: Int, // previous fibonacci number
        step: Int // the iteration
      ): IO[ActivityCompletionException, BigDecimal] =
        if (step >= n) ZIO.succeed(sum)
        else
          for {
            // Performs heartbeats
            _ <- context.heartbeat(HeartbeatDetails(sum, curr, prev, step))
            _ <- ZIO.logInfo("Sleep...")
            // Some delays for demo purposes
            _ <- ZIO.sleep((50 * step).millis)
            _       = iterationsCounter.incrementAndGet()
            newStep = step + 1
            newPrev = curr
            newCurr = curr + prev
            newSum  = sum + curr
            res <- iter(newSum, newCurr, newPrev, newStep)
          } yield res

      for {
        // Recovers the previous heartbeat details
        heartbeatDetails <- context.getHeartbeatDetails[HeartbeatDetails]
        progress = heartbeatDetails.getOrElse(
                     HeartbeatDetails(sum = 0, curr = 1, prev = 0, step = 0)
                   )
        res <- iter(progress.sum, progress.curr, progress.prev, progress.step)
      } yield res
    }
}
```

You can now simply test that the activity uses heartbeats:

```scala mdoc:silent
object FibonacciHeartbeatActivityActivitySpec extends ZIOSpecDefault {
  override val spec = suite("FibonacciHeartbeatActivity")(
    test("performs heartbeats message") {
      ZTestActivityEnvironment.activityOptions[Any].flatMap { implicit options =>
        // Initialize a fresh counter
        val numIterations = new AtomicInteger()
        for {
          _ <- ZTestActivityEnvironment.addActivityImplementation(new FibonacciHeartbeatActivityImpl(numIterations))
          stub <- ZTestActivityEnvironment
            .newActivityStub[FibonacciHeartbeatActivity]
            .withStartToCloseTimeout(1.minute)
            .build
          // Set heartbeat details as if 3 iterations were performed
          _ <- ZTestActivityEnvironment.setHeartbeatDetails(
            FibonacciHeartbeatActivityImpl.HeartbeatDetails(
              sum = 4,
              curr = 3,
              prev = 2,
              step = 3
            )
          )
          // Calculating takes 5 iterations
          result = stub.fibonacciSum(5)
        } yield {
          assertTrue(result == 12) &&
          // Performs only 2 iterations
          assertTrue(numIterations.get() == 2)
        }
      }
    }
  ).provide(
    ZTestEnvironmentOptions.default,
    ZTestActivityEnvironment.make[Any]
  ) @@ TestAspect.withLiveClock
}
```

**Notes**
- `ZTestActivityEnvironment.setHeartbeatDetails` sets the heartbeat details before the activity is invoked
- Activity implementation will pick up the specified heartbeat details once run
