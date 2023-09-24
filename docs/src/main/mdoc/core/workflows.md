# Workflows

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal workflows" />
  <meta name="keywords" content="ZIO Temporal workflows, Scala Temporal workflows" />
</head>

## Introduction

Workflows are the basic building blocks in Temporal.  
A Workflow Definition contains the actual business logic. It's determenistic, those free from any side effects.  
Refer to [Temporal documentation](https://docs.temporal.io/workflows) for more information regarding the concept of
workflows.

## Defining workflows

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._

import java.util.UUID
```

Every Temporal application starts from Workflow Definitions.  
A simple workflow consists of the following parts:

- A Scala *trait* with an abstract method
- The *trait* itself should be annotated with `@workflowInterface`
- The method should be annotated with `@workflowMethod`

```scala mdoc:silent
@workflowInterface
trait EchoWorkflow {

  @workflowMethod
  def echo(str: String): String
}
```

Implementing simple workflows is as simple as just plain interfaces:

```scala mdoc:silent
class EchoWorkflowImpl extends EchoWorkflow {
  override def echo(str: String): String = {
    println(s"Echo: $str")
    str
  }
}
```

That's it!

## Executing workflows

First, you should connect to the Temporal cluster. This is done via the Workflow client.  
Any workflow run requires providing some mandatory parameters, such as the unique Workflow ID, the Task Queue, and optionally others (such as timeouts).  
The configuration is passed using `ZWorkflowOptions`:

```scala mdoc
val workflowOptions = ZWorkflowOptions
  .withWorkflowId(UUID.randomUUID().toString)
  .withTaskQueue("echo-queue")
  .withWorkflowRunTimeout(10.second)
```

In order to run a specific workflow, you should create a Workflow stub:

```scala mdoc:silent
def createWorkflowStub(workflowClient: ZWorkflowClient): UIO[ZWorkflowStub.Of[EchoWorkflow]] = 
  workflowClient.newWorkflowStub[EchoWorkflow](workflowOptions)
```

Important notes:

- You need a `ZWorkflowClient` instance to build a Workflow stub. You should either access it via ZIO environment or
  have it already created somewhere
- In `client.newWorkflowStub` method you provide the specific workflow interface you will work with
  - The method requires specifying the Workflow Interface type and `ZWorkflowOptions`

Having a Workflow stub, you'll be able to execute the workflow. Executing means starting the workflow and waiting for
its completion:

```scala mdoc:silent
val workflowResultZIO = 
  for {
    echoWorkflow <- ZIO.serviceWithZIO[ZWorkflowClient](createWorkflowStub)
    result       <- ZWorkflowStub.execute(echoWorkflow.echo("Hello there"))
  } yield result
```

Important notes:

- **You must always** wrap the workflow method invocation into `ZWorkflowStub.execute` method.
  - The `ZWorkflowStub.Of[EchoWorkflow]` is a compile-time stub, so actual method invocations are only valid in compile-time
  - `echoWorkflow.echo("Hello there")` invocation would be re-written into an untyped Temporal's workflow invocation
    (see [workflow Execution doc](https://docs.temporal.io/application-development/foundations?lang=java#start-workflow-execution))
  - A direct method invocation will throw an exception
- The `ZWorkflowStub` is basically a proxy, which executes the Workflow via Temporal cluster

**NOTE**: Do not annotate workflow stubs with the workflow interface type. It must be `ZWorkflowStub.Of[EchoWorkflow]`.  
Otherwise, you'll get a compile-time error:  

```scala mdoc:fail
def doSomething(echoWorkflow: EchoWorkflow): TemporalIO[String] =
  ZWorkflowStub.execute(echoWorkflow.echo("Hello there"))
```

## Workers

In this section, we'll be referring to Worker Entity as a Worker.  
The Worker runs a workflow which someone executed.

We assume that a Worker Process consists only of one Worker Entity listening to a single Task Queue.  
Refer to [Temporal documentation](https://docs.temporal.io/workers) for more information regarding workers and task
queues.

A Worker itself depends on a Workflow client which communicates with the Temporal cluster.  
Using the Workflow client, it's now possible to create a Worker factory, which itself creates the Worker.

```scala mdoc:silent
val worker: URLayer[ZWorkerFactory, Unit] = ZLayer.fromZIO {
  ZIO.serviceWithZIO[ZWorkerFactory] { workerFactory =>
    for {
      worker <- workerFactory.newWorker("echo-queue")
      _ <- worker
              .addWorkflow[EchoWorkflow]
              .from(new EchoWorkflowImpl)
    } yield ()
  }
}
```

There is also an alternative syntax for building workflows which relies on *ZIO Aspects*:  
```scala mdoc:silent
val workerAspects: URLayer[ZWorkerFactory, Unit] = ZLayer.fromZIO {
  ZWorkerFactory.newWorker("echo-queue") @@
    ZWorker.addWorkflow[EchoWorkflow].from(new EchoWorkflowImpl)
}.unit
```

This syntax allows avoiding syntactic noise of monadic composition and accessing ZIO's environment.  
Therefore, **it's a preferred one**.  

Important notes:

- You need a `ZWorkerFactory` instance to create a worker. You should either access it via ZIO environment or have it
  already created somewhere
- You must specify the **task queue** when creating a worker (`echo-queue` in this case).
- Make sure that worker uses the same **task queue** as the client code  
- In `worker.addWorkflow` method you provide the specific workflow interface you run in this worker
- It's required to provide the workflow implementation instance there.
- Create the workflow implementation **only inside** the `addWorkflow` method, otherwise an exception will be thrown.

## Running the Worker and the Workflow client
Let's bring all the parts into a program:

```scala mdoc:silent
val program = 
  for {
    _              <- ZWorkerFactory.setup
    workflowResult <- workflowResultZIO
    _              <- ZIO.log(s"The workflow result: $workflowResult")
  } yield ()
```

When injecting, don't forget to provide the worker:

```scala
program.provide(
  worker,
  // Other dependencies
)
```
