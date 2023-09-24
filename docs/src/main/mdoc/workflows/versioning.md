# Versioning

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal versioning" />
  <meta name="keywords" content="ZIO Temporal versioning, Scala Temporal versioning" />
</head>


As outlined in the Workflow Implementation Constraints section, Workflow code has to be deterministic by taking the same
code path when replaying history events.  
Any Workflow code change that affects the order in which commands are generated breaks this assumption. The solution
that allows updating code of already running Workflows is to keep both the old and new code.  
When replaying, use the code version that the events were generated with and when executing a new code path, always take
the new code.

## Worker versioning

To use Worker Versioning, you need to do the following:

1. Determine and assign a Build ID to your built Worker code, and opt in to versioning.
2. Tell the Task Queue your Worker is listening on about that Build ID, and whether its compatible with an existing
   Build ID.

### Assign a Build ID to your Worker

Let's say you've chosen `borsch` as your Build ID, which might be a short git commit hash (a reasonable choice as Build
ID).  
To assign it in your Worker code, assign the following Worker Options:

```scala mdoc:silent
import zio.temporal.worker._

val options = ZWorkerOptions.default.withBuildId("borsch")

val worker = ZWorkerFactory.newWorker("<task-queue>", options)
// ...
```

That's all you need to do in your Worker code. Importantly, if you start this Worker, it won't receive any tasks.  
That's because you need to tell the Task Queue about your Worker's Build ID first.

### Tell the Task Queue about your Worker's Build ID

Now you can use the SDK (or the Temporal CLI) to tell the Task Queue about your Worker's Build ID. You might want to do
this as part of your CI deployment process.

How to do it via Temporal CLI:

```shell
temporal task-queue update-build-ids add-new-default --build-id borsch --task-queue "<task-queue>"
```

How to do it via SDK:

```scala mdoc:silent
import zio._
import zio.temporal.workflow._
import io.temporal.client._

ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient.updateWorkerBuildIdCompatibility("<task-queue>", BuildIdOperation.newIdInNewDefaultSet("borsch"))
}
```

This code adds the `borsch` Build ID to the Task Queue as the sole version in a new version set, which becomes the
default for the queue. New Workflows execute on Workers with this Build ID, and existing ones will continue to process
by appropriately compatible Workers.

If, instead, you want to add the Build ID to an existing compatible set, you can do this:

```scala mdoc:silent
ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient.updateWorkerBuildIdCompatibility("<task-queue>", BuildIdOperation.newCompatibleVersion("borsch", "some-existing-build-id"))
}
```

This code adds `borsch` to the existing compatible set containing `some-existing-build-id` and marks it as the new
default Build ID for that set.

You can also promote an existing Build ID in a set to be the default for that set:

```scala mdoc:silent
ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient.updateWorkerBuildIdCompatibility("<task-queue>", BuildIdOperation.promoteBuildIdWithinSet("borsch"))
}
```

You can also promote an entire set to become the default set for the queue. New Workflows will start using that set's
default.

```scala mdoc:silent
ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient.updateWorkerBuildIdCompatibility("<task-queue>", BuildIdOperation.promoteSetByBuildId("borsch"))
}
```

### Specify versions for Commands

By default, Activities, Child Workflows, and Continue-as-New use the same compatible version set as the Workflow that
invoked them if they're also using the same Task Queue.

If you want to override this behavior, you can specify your intent via the `withVersioningIntent` method on the activity
options, child workflow options, or continue-as-new options objects.

For example, if you want to use the latest default version for an Activity, you can define your Activity Options like
this:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import io.temporal.common.VersioningIntent

@activityInterface
trait MyActivity {
  // ... some activity methods
}

@workflowInterface
trait MyWorkflow {
  @workflowMethod
  def doSomething(): Unit
}

class MyWorkflowImpl extends MyWorkflow {
   private val activity = ZWorkflow.newActivityStub[MyActivity](
      ZActivityOptions
        .withStartToCloseTimeout(5.seconds)
        .withVersioningIntent(VersioningIntent.VERSIONING_INTENT_DEFAULT)
        // ...other options
   ) 
  
   override def doSomething(): Unit = ???
}
// Building the activity stub
```