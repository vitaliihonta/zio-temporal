# Workers

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal workers" />
  <meta name="keywords" content="ZIO Temporal workers, Scala Temporal workers" />
</head>

## Building workers
Often, the worker application runs more than just one workflow and one activity.  
Let's take a look at the following example:
```scala mdoc:silent
import zio._
import zio.temporal._

// Activity 1
@activityInterface
trait SampleActivity {}
class SampleActivityImpl extends SampleActivity {/*...*/}

// Workflow 1
@workflowInterface
trait WorkflowA {
  @workflowMethod
  def methodA(): Unit
}
class WorkflowAImpl extends WorkflowA {
  override def methodA() = ??? /*...*/
}

// Activity 2
@activityInterface
trait AnotherActivity {}
class AnotherActivityImpl extends AnotherActivity {/*...*/}

// Workflow 2
@workflowInterface
trait WorkflowB {
  @workflowMethod
  def methodB(): Unit
}
class WorkflowBImpl extends WorkflowB {
  override def methodB() = ??? /*...*/
}

// Workflow 3
@workflowInterface
trait WorkflowC {
  @workflowMethod
  def methodC(): Unit
}
class WorkflowCImpl extends WorkflowC {
  override def methodC() = ??? /*...*/
}
```

Typical code constructing a Worker running all of those Temporal units looks like this:

```scala mdoc:silent
import zio.temporal.worker._
import zio.temporal.workflow._

ZWorkerFactory.newWorker("<task-queue>") @@
  ZWorker.addWorkflow[WorkflowAImpl].fromClass @@
  ZWorker.addWorkflow[WorkflowBImpl].fromClass @@
  ZWorker.addWorkflow[WorkflowCImpl].fromClass @@
  ZWorker.addActivityImplementation(new SampleActivityImpl) @@
  ZWorker.addActivityImplementation(new AnotherActivityImpl)
```

It might satisfy most of your needs. However, in case the number of workflows and activities grows in your application,  
you might need a way to provide a bigger number of workflows easily. This is when `ZWorkflowImplementationClass` and `ZActivityImplementationObject` helpers come to the rescue:

```scala mdoc
val workflowClasses: List[ZWorkflowImplementationClass[_]] = List(
  ZWorkflowImplementationClass[WorkflowAImpl],
  ZWorkflowImplementationClass[WorkflowBImpl],
  ZWorkflowImplementationClass[WorkflowCImpl],
  /* ... other workflows */
)

import zio.temporal.activity._

val activityImplementations: List[ZActivityImplementationObject[_]] = List(
  ZActivityImplementationObject(new SampleActivityImpl),
  ZActivityImplementationObject(new AnotherActivityImpl),
  /* ... other activities */
)
```

Then, the worker can be constructed as follows:
```scala mdoc:silent
ZWorkerFactory.newWorker("<task-queue>") @@
  ZWorker.addWorkflowImplementations(workflowClasses) @@
  ZWorker.addActivityImplementations(activityImplementations)
```

**Important notes**:
- `ZWorkflowImplementationClass` helper checks if the wrapped type is a correct workflow implementation at compile time. Classes not annotated properly or with a missing public nullary constructors won't compile.
- `ZActivityImplementationObject` helper checks if the wrapped type is a correct activity implementation at compile time. Classes not annotated properly won't compile.

Here is an example of compilation errors:  

*Case 1*: missing @workflowInterface
```scala mdoc:fail
class Foo {}
ZWorkflowImplementationClass[Foo]
```

*Case 2*: missing public constructor
```scala mdoc:fail
@workflowInterface
class Bar private() {}

ZWorkflowImplementationClass[Bar]
```

*Case 3*: not an activity interface
```scala mdoc:fail
class Borscht {}

ZActivityImplementationObject(new Borscht)
```


### ZLayer Interop
It is usual for activities to have dependencies. The `ZActivityImplementationObject` can also be constructed from a ZLayer:

```scala mdoc
val sampleActivityLayer: ULayer[SampleActivityImpl] =
  ZLayer.succeed(new SampleActivityImpl)

val anotherActivityLayer: ULayer[AnotherActivityImpl] =
  ZLayer.succeed(new AnotherActivityImpl)
  
val activitiesLayer: ULayer[List[ZActivityImplementationObject[_]]] = 
  ZLayer.collectAll(
    List(
      ZActivityImplementationObject.layer(sampleActivityLayer),
      ZActivityImplementationObject.layer(anotherActivityLayer)
    )
  )
```

The same worker can then be constructed as follows:
```scala mdoc:silent
ZWorkerFactory.newWorker("<task-queue>") @@
  ZWorker.addWorkflowImplementations(workflowClasses) @@
  ZWorker.addActivityImplementationsLayer(activitiesLayer)
```