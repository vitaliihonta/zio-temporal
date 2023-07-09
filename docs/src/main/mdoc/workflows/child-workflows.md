# Child workflows

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal child workflows" />
  <meta name="keywords" content="ZIO Temporal child workflows, Scala Temporal child workflows" />
</head>

A [Child workflow](https://docs.temporal.io/workflows#child-workflow) is a Workflow Execution that is spawned from within another Workflow.  
A Workflow Execution can be both a Parent and a Child Workflow Execution because any Workflow can spawn another Workflow.  

## Defining child workflows

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.workflow._
import java.util.UUID
```

Then define workflow interfaces:

```scala mdoc:silent
// Child Workflow interface
@workflowInterface
trait GreetingChild {
  @workflowMethod
  def composeGreeting(greeting: String, name: String): String

  // Note the signal method
  @signalMethod
  def updateName(name: String): Unit
}

// Parent Workflow interface
@workflowInterface
trait GreetingWorkflow {
  def getGreeting(name: String): String
}
```

### Synchronous invocation

The simplest case of using child workflows is invoking them synchronously using `ZChildWorkflowStub.execute`:
```scala mdoc:silent
// Simple parent Workflow implementation
class GreetingWorkflowSimple extends GreetingWorkflow {
  override def getGreeting(name: String): String = {
    val child: ZChildWorkflowStub.Of[GreetingChild] = 
      ZWorkflow.newChildWorkflowStub[GreetingChild].build

    ZChildWorkflowStub.execute(
      child.composeGreeting("Hello", name)
    )
  }
}
```

- To create a child workflow stub, you must use `ZWorkflow.newChildWorkflowStub[<ChildType>]` method.
  - It's possible to configure the child workflow because `newChildWorkflowStub` returns a builder
- **Reminder: you must always** wrap the child workflow invocation into `ZChildWorkflowStub.execute` method.
    - `child.composeGreeting("Hello", name)` invocation would be re-written into an untyped Temporal's workflow invocation
    - A direct method invocation will throw an exception

**NOTE**: Do not annotate workflow stubs with the workflow interface type. It must be `ZWorkflowStub.Of[EchoWorkflow]`.  
Otherwise, you'll get a compile-time error:

```scala mdoc:fail
def doSomething(child: GreetingChild): String =
  ZChildWorkflowStub.execute(child.composeGreeting("Hello", "World"))
```

### Asynchronous invocation
It's also possible to spawn multiple child workflows and running them in parallel using `ZChildWorkflowStub.executeAsync`:

```scala mdoc:silent
// Parent Workflow implementation
class GreetingWorkflowParallel extends GreetingWorkflow {
  
  override def getGreeting(name: String): String = {

    // Workflows are stateful, so a new stub must be created for each new child.
    val child1 = ZWorkflow.newChildWorkflowStub[GreetingChild].build
    // The result of the async invocation
    val greeting1: ZAsync[String] = ZChildWorkflowStub.executeAsync(
      child1.composeGreeting("Hello", name)
    )

    // Both children will run concurrently.
    val child2 = ZWorkflow.newChildWorkflowStub[GreetingChild].build
    val greeting2 = ZChildWorkflowStub.executeAsync(
      child2.composeGreeting("Bye", name)
    )

    // You can combine ZAsync instances like ZIO
    val result: ZAsync[String] = greeting1.zipWith(greeting2)(
      (first, second) =>
        s"First: $first , second: $second"
    )
    
    // Block until async computation completes
    result.run.getOrThrow
  }
}
```
### Signals
It's possible to send a Signal to a Child Workflow from the parent using `ZChildWorkflowStub.signal`:
```scala mdoc:silent
// Parent Workflow implementation
class GreetingWorkflowSignaling extends GreetingWorkflow {

  override def getGreeting(name: String): String =  {
    val child = ZWorkflow.newChildWorkflowStub[GreetingChild].build
    
    // Execute the child asynchronously
    val greeting = ZChildWorkflowStub.executeAsync(
      child.composeGreeting("Hello", name)
    )
    
    // Send the signal to the child
    ZChildWorkflowStub.signal(
      child.updateName("Temporal")
    )

    // Block until async computation completes
    greeting.run.getOrThrow
  }
}
```

**NOTE** Sending a Query to Child Workflows from within the parent Workflow code is not supported.  
However, you can send a Query to Child Workflows from Activities using WorkflowClient.