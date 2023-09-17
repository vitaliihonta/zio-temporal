# Workflows

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal testing workflows" />
  <meta name="keywords" content="ZIO Temporal testing workflows, Scala Temporal testing workflows" />
</head>

Temporal provides with `ZTestWorkflowEnvironment` that allows to run workflows in a local test environment.  
General business logic, as well Temporal functionality (such as `timers`, `sagas`, etc.) can be tested locally with the testkit.

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.test._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.worker._
import zio.temporal.testkit._
```

## Simple tests
Imagine the following Workflow interface:

```scala mdoc:silent
@workflowInterface
trait SampleWorkflow {

  @workflowMethod
  def echo(str: String): String
}

class SampleWorkflowImpl() extends SampleWorkflow {
  override def echo(str: String): String = 
    s"Echoed $str"
}
```

Here is an example of testing this workflow:

```scala mdoc:silent
object SampleWorkflowSpec extends ZIOSpecDefault {
  override val spec = suite("SampleWorkflow")(
    test("Echoes message") {
      val taskQueue = "sample-queue"
      for {
        // Create the worker
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
          ZWorker.addWorkflow[SampleWorkflowImpl].fromClass
        
        // Setup the test environment
        _ <- ZTestWorkflowEnvironment.setup()
        
        // Create a workflow stub
        sampleWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[SampleWorkflow](
                            ZWorkflowOptions
                              .withWorkflowId("sample-workflow-id")
                              .withTaskQueue(taskQueue)
                              // Set workflow timeout
                              .withWorkflowRunTimeout(10.second)
                          )
        
        // Execute the workflow stub
        result <- ZWorkflowStub.execute(sampleWorkflow.echo("Hello"))
      } yield assertTrue(result == "Echoed Hello")
    }
  ).provideSome[Scope](
    ZTestEnvironmentOptions.default,
    ZTestWorkflowEnvironment.make[Any]
  ) @@ TestAspect.withLiveClock
}
```

**Notes**
- To test the workflow, it's required to configure & setup the test environment:
  - `ZTestWorkflowEnvironment.newWorker` creates a new worker listening the specified task queue
  - It's possible to add live Workflows and Activities using ZIO's aspect-based syntax (`@@` operator)
    - `ZWorker.addWorkflow` adds a workflow to the worker
    - Any other method (such as `ZWorker.addActivityImplementation`) is also allowed here
  - `ZTestWorkflowEnvironment.setup` starts the test environment
  - `ZTestWorkflowEnvironment.newWorkflowStub` creates a workflow stub for testing
- It's recommended to use `TestAspect.withLiveClock` in your tests to avoid hanging tests
    - It may happen because both zio-test & temporal-testkit use their own virtual clock

## Workflows with activities
It's also pretty easy to test Workflows that use activities.

Imagine the following activity:

```scala mdoc:silent
import zio.temporal.activity._

@activityInterface
trait GreetingActivity {
  def composeGreeting(greeting: String, name: String): String
}

// Activity runs ZIO
class GreetingActivityImpl(implicit options: ZActivityRunOptions[Any]) extends GreetingActivity {
  override def composeGreeting(greeting: String, name: String): String = {
    ZActivity.run {
      ZIO.logInfo(s"Composing greeting=$greeting name=$name") *>
        ZIO.succeed(s"$greeting $name")
    }
  }
}
```

And the workflow using this activity:

```scala mdoc:silent
@workflowInterface
trait GreetingWorkflow {
  @workflowMethod
  def greet(name: String): String
}

class GreetingWorkflowImpl extends GreetingWorkflow {
  private val activity = ZWorkflow.newActivityStub[GreetingActivity](
    ZActivityOptions.withStartToCloseTimeout(5.seconds)
  )
  
  override def greet(name: String): String = {
    ZActivityStub.execute(
      activity.composeGreeting("Hello", name)
    )
  }
}
```

Here is an example of testing this workflow:

```scala mdoc:silent
object GreetingWorkflowSpec extends ZIOSpecDefault {
  override val spec = suite("GreetingWorkflow")(
    test("Composes greeting message") {
      val taskQueue = "greeting-queue"
      // Get ZActivityOptions for the activity
      ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit options =>
        for {
          // Create the worker
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
            ZWorker.addWorkflow[GreetingWorkflowImpl].fromClass @@
            ZWorker.addActivityImplementation(new GreetingActivityImpl())

          // Setup the test environment
          _ <- ZTestWorkflowEnvironment.setup()

          // Create a workflow stub
          greetingWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[GreetingWorkflow](
                                ZWorkflowOptions
                                  .withWorkflowId("greeting-workflow-id")
                                  .withTaskQueue(taskQueue)
                                  // Set workflow timeout
                                  .withWorkflowRunTimeout(10.second)
                              )

          // Execute the workflow stub
          result <- ZWorkflowStub.execute(greetingWorkflow.greet("World"))
        } yield assertTrue(result == "Hello Hello")
      }
    }
  ).provideSome[Scope](
    ZTestEnvironmentOptions.default,
    ZTestWorkflowEnvironment.make[Any]
  ) @@ TestAspect.withLiveClock
}
```
**Notes**
- Like `ZTestActivityEnvironment`, `ZTestWorkflowEnvironment.activityRunOptions[R]` provides with `ZActivityOptions` needed to run ZIO inside activities
  - `ZTestWorkflowEnvironment.activityRunOptionsWithZIO[R]` allows building a ZIO accessing `ZActivityRunOptions` (like `ZIO.serviceWithZIO` for ZIO environment)
- `ZWorker.addActivityImplementation` can be used to provide activity implementations

## Time skipping
Some long-running Workflows can persist for months or even years. Implementing the test framework allows your Workflow code to skip time and complete your tests in seconds rather than the Workflow's specified amount.

For example, if you have a Workflow sleep for a day, or have an Activity failure with a long retry interval, you don't need to wait the entire length of the sleep period to test whether the sleep function works.  
Instead, test the logic that happens after the sleep by skipping forward in time and complete your tests in a timely manner.

The test framework included in most SDKs is an in-memory implementation of Temporal Server that supports skipping time.  
Time is a global property of an instance of `ZTestWorkflowEnvironment`: skipping time (either automatically or manually) applies to all currently running tests. If you need different time behaviors for different tests, run your tests in a series or with separate instances of the test server.  
For example, you could run all tests with automatic time skipping in parallel, and then all tests with manual time skipping in series, and then all tests without time skipping in parallel.  

Imagine the following long-running workflow:

```scala mdoc:silent
import zio.temporal.state._

@workflowInterface
trait PaymentWorkflow {
  // Returns true if confirmed
  @workflowMethod
  def processPayment(amount: BigDecimal): Boolean
  
  @signalMethod
  def confirm(): Unit
}

class PaymentWorkflowImpl extends PaymentWorkflow {

  private val isConfirmed = ZWorkflowState.make(false)

  override def processPayment(amount: BigDecimal): Boolean = {
    // Waits 10 minutes for the payment confirmation
    // Unblocks whenever the condition is met
    ZWorkflow.awaitUntil(10.minutes)(
      isConfirmed =:= true
    )
    isConfirmed.snapshot
  }

  override def confirm(): Unit = {
    // Confirmed!
    isConfirmed := true
  }
}
```

It's possible to avoid waiting `10 minutes` for the workflow to complete. Here is how to do it:

```scala mdoc:silent
object PaymentWorkflowSpec extends ZIOSpecDefault {
  override val spec = suite("Payment")(
    test("Cancels the payment after 10 minutes") {
      val taskQueue = "payment-queue"
      for {
        // Create the worker
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
          ZWorker.addWorkflow[PaymentWorkflowImpl].fromClass

        // Setup the test environment
        _ <- ZTestWorkflowEnvironment.setup()

        // Create a workflow stub
        paymentWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[PaymentWorkflow](
                             ZWorkflowOptions
                               .withWorkflowId("payment-workflow-id")
                               .withTaskQueue(taskQueue)
                               // Set workflow timeout
                               .withWorkflowRunTimeout(20.minutes)
                           )

        // Start the workflow stub
        _ <- ZWorkflowStub.start(
          paymentWorkflow.processPayment(42.1)
        )
        // Skip time
        _ <- ZTestWorkflowEnvironment.sleep(10.minutes + 30.seconds)
        // Get the workflow result
        isFinished <- paymentWorkflow.result[Boolean]
      } yield assertTrue(!isFinished)
    }
  ).provideSome[Scope](
    ZTestEnvironmentOptions.default,
    ZTestWorkflowEnvironment.make[Any]
  ) @@ TestAspect.withLiveClock
}
```

**Notes**
- First, start the workflow asynchronously using `ZWorkflowStub.start`
- Then run `ZTestWorkflowEnvironment.sleep` to perform time skipping
- Get the result using `paymentWorkflow.result[<Type>]`

