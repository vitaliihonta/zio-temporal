# FAQ

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
import zio.temporal.*
import zio.temporal.activity.*

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
import zio.*
import zio.temporal.workflow.*

class EchoWorkflowImpl extends EchoWorkflow {
  // Inside some workflow
  private val activity: ZActivityStub.Untyped = ZWorkflow.newUntypedActivityStub
    .withStartToCloseTimeout(5.seconds)
    .build

  override def echoWorkflow(what: String): String = {
    activity.execute("Echo", what)
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
  } yield ()
}
```