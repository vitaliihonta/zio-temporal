# Workflow Definitions

We have already briefly mentioned Temporal Workflow Definitions.
Now let's look at them in more detail.

A Temporal Workflow is defined in Scala by a trait, along with a class that implements that trait.  The trait is annotated in a way that Temporal will recognize it as representing a Workflow.  For example:

```scala
import zio.temporal.*

@workflowInterface
trait HelloWorld:

  @workflowMethod
  def apply(name: String): String

class HelloWorldImpl extends HelloWorld:

  override def apply(name: String) = s"Hello $name!"
```

The trait is annotated with [`@workflowInterface`](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/WorkflowInterface.html) and the method that Temporal invokes to start the Workflow is annotated [`@workflowMethod`](https://www.javadoc.io/doc/io.temporal/temporal-sdk/latest/io/temporal/workflow/WorkflowMethod.html), in this case `apply()`.  There can be only one such annotated method per Workflow Definition, so an application with two Workflows requires having two separate annotated traits.

These annotations are provided by the [zio.temporal](https://zio-temporal.vhonta.dev/api/zio/temporal/) package, and so can be imported with `import zio.temporal.*`.

## Workflows, Tasks, and Activities

Conceptially, a Workflow is a sequence of steps to be performed until completion or failure.  These steps are called _Tasks_, and are of two sorts: [Workflow Tasks](https://docs.temporal.io/workers#workflow-task) and [Activity Tasks](https://docs.temporal.io/workers#activity-task).  By analogy, Workflow Tasks correspond to pure functions: they are deterministic and idempotent with no external side effects.  Activity Tasks correspond to impure functions because they can have side effects.

Temporal needs to distinguish these two types of tasks for the purpose of replaying a Workflow when necessary.  The results of Activities are persisted on the Temporal server so that if the Worker crashes, for example, the result of any completed Activity is not lost and the Workflow can resume without repeating any side-effects that produced the value returned by the Activity.  On the other hand, Workflow Tasks are deterministic and therefore safe to repeat; thus their results are not persisted on the Temporal Server but rather re-calculated if a Workflow is replayed.

Tasks that interact with outside services, such as reading a file, accessing a database, or communicating with a network service must be defined as Activities.

## Activity Definitions

Activities are defined similarly to Workflows: as an annotated trait whose methods declare Activity Tasks, along with an implementing class that has definitions for those Tasks.  Activity Definitions differ from Workflow Definitions in several ways:

1. Activity methods need not be annotated.
1. More than one Activity can be declared in an Activity trait.
1. Your program does not invoke Activities directly, rather it invokes a Workflow, and the Workflow invokes the Activities.

Put another way, an Activity is _part of_ a Workflow.  A ZIO Temporal application can exist without any Activities defined, but it must have at least one Workflow Definition.  We will see an example of defining and executing an Activity in a later section of this tutorial.

The work of a Workflow is performed by a Temporal Worker.  Now that we have learned how to define Workflows we will learn how to start a Worker.