# FAQ

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal FAQ" />
  <meta name="keywords" content="ZIO Temporal FAQ, ZIO Temporal troubleshooting" />
</head>

## What's zio.temporal.internal.StubProxies$IllegalStubProxyInvocationException?

An exception like this one
```
zio.temporal.internal.StubProxies$IllegalStubProxyInvocationException: interface com.example.payments.workflows.PaymentActivity methods should not be invoked at runtime!
It's likely that you forgot to wrap Workflow/Activity calls
into ZWorkflowStub.execute/ZActivityStub.execute blocks, etc.
Method was invoked: public abstract com.example.transactions.TransactionView com.example.payments.workflows.PaymentActivity.proceed(com.example.transactions.ProceedTransactionCommand) throws com.example.payments.workflows.BankError
```
Actually means what it says: **you forgot to wrap** Workflow or Activity interaction.  
Reminder: **you must always** wrap the workflow/activity interactions into `ZWorkflowStub.execute`, `ZActivityStub.execute`, `ZWorkflowStub.signal`, `ZWorkflowStub.query` methods, etc.
- The `ZWorkflowStub.Of[A]`, `ZChildWorkflowStub.Of[A]`, `ZActivityStub.Of[A]`, etc. are all compile-time stubs, so their method invocations are only valid in compile-time
- Scala's method invocation would be re-written into an untyped Temporal's activity invocation


## Typical problems when using zio-temporal-testkit with zio-test

### Test clock doesn't work well

If test is failing with such an error:

```
ERROR i.t.t.TestActivityEnvironmentInternal - Timeout trying execute activity task task_token: "test-task-token"
```

Try to look for ZIO warnings like this one:

```
Warning: A test is using time, but is not advancing the test clock, which may result in the test hanging. Use TestClock.adjust to manually advance the time.
```

It is likely that the Activity code is running ZIO with `ZIO.sleep`.  
In a combination with Temporal and zio-test, it may hang due to `TestClock`.

**You can easily fix it** by replacing `TestClock` with a real one:

```scala
test("my test") {
  /*...*/
} @@ TestAspect.withLiveClock // Add the aspect
```

## ZIO-Temporal uses Scala macros heavily. How to inspect the generated code?

Add the following VM parameter to SBT:

```
-Dzio.temporal.debug.macro=true
```

It will print the generated code. For example:

```shell
[info] -- Info: /Users/vitaliihonta/IdeaProjects/zio-temporal/integration-tests/src/test/scala/zio/temporal/WorkflowSpec.scala:213:25 
[info] 213 |        thirdSnapshot <- ZWorkflowStub.query(
[info]     |                         ^
[info]     |Generated query invocation tree=_root_.zio.temporal.internal.TemporalInteraction.from[scala.collection.immutable.List[scala.Predef.String]](zio.temporal.internal.TemporalWorkflowFacade.query[scala.collection.immutable.List[scala.Predef.String]](workflowStub.toJava, "messages", scala.Nil)(ctg$proxy8))
[info] 214 |                           workflowStub.messages
[info] 215 |                         )
```

## Typed and Untyped method names

For some reason, workflow-related methods (`@signalMethod`, `@queryMethod`) are registered as is in
Temporal,  
while activity method are registered capitalized.

For instance, having the following activity interface:

```scala mdoc:silent
import zio.temporal._
import zio.temporal.activity._

@activityInterface
trait EchoActivity {
  def echo(what: String): String
}

@workflowInterface
trait EchoWorkflow {
  @workflowMethod
  def echoWorkflow(what: String): String

  @signalMethod
  def signalSomething(): Unit
}
```

The activity name in untyped invocation must be `Echo` (not `echo`):

```scala mdoc:silent
import zio._
import zio.temporal.workflow._

class EchoWorkflowImpl extends EchoWorkflow {
  // Inside some workflow
  private val activity: ZActivityStub.Untyped = ZWorkflow.newUntypedActivityStub
    .withStartToCloseTimeout(5.seconds)
    .build

  override def echoWorkflow(what: String): String = {
    activity.execute[String]("Echo", what)
  }

  @signalMethod
  override def signalSomething(): Unit = {
    // Do nothing, it's just for demo purposes
  }
}
```

On the other hand, an untyped workflow signal must be invoked with the same lower-cased name:

```scala mdoc:silent
ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  for {
    echoWorkflow <- workflowClient.newUntypedWorkflowStub("EchoWorkflow")
      .withTaskQueue("task-queue")
      .withWorkflowId("workflow-id-a1b2c3")
      .build

    _ <- echoWorkflow.start("HELLO THERE")
    _ <- echoWorkflow.signal("signalSomething")
    // get result
    result <- echoWorkflow.result[String]
  } yield ()
}
```
