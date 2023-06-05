# Continue as new
[Continue-As-New](https://docs.temporal.io/workflows#continue-as-new) enables a Workflow Execution to close successfully and create a new Workflow Execution in a single atomic operation if the number of Events in the Event History is becoming too large.  
The Workflow Execution spawned from the use of Continue-As-New has the same Workflow Id, a new Run Id, and a fresh Event History and is passed all the appropriate parameters.
ZIO-Temporal allows you to use Continue-As-New in various ways.  

## Defining a stub

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.workflow._
import java.util.UUID
```

Then define workflow interfaces:

```scala mdoc:silent
@workflowInterface
trait LongRunningWorkflow {
  @workflowMethod
  def watchFiles(paths: List[String]): Unit
}
```

In order to Continue-As-New, it's required to define a `ZWorkflowContinueAsNewStub` and run it using `ZWorkflowContinueAsNewStub.execute`:
```scala mdoc:silent
class LongRunningWorkflowImpl extends LongRunningWorkflow {
  private val logger = ZWorkflow.makeLogger
  
  private val nextRun = ZWorkflow.newContinueAsNewStub[LongRunningWorkflow].build
  
  override def watchFiles(paths: List[String]): Unit = {
    logger.info(s"Watching files=$paths")
    // Do stuff
    ZWorkflowContinueAsNewStub.execute(
      nextRun.watchFiles(paths)
    )
  }
}
```

- To create a Continue-As-New stub, you must use `ZWorkflow.newContinueAsNewStub[<Type>]` method.
  - It's possible to configure it because `newChildWorkflowStub` returns a builder
- **Reminder: you must always** wrap the Continue-As-New invocation into `ZWorkflowContinueAsNewStub.execute` method.
    - `nextRun.watchFiles(paths)` invocation would be re-written into an untyped Temporal's Continue-As-New call
    - A direct method invocation will throw an exception