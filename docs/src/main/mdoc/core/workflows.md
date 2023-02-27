# Workflows

## Introduction

Workflows are the basic building blocks in Temporal.  
A Workflow Definition contains the actual business logic. It's determenistic, those free from any side effects.  
Refer to [Temporal documentation](https://docs.temporal.io/workflows) for more information regarding the concept of
workflows.

## Defining workflows

Let's start from some basic imports that will be required for the whole demonstration:

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
In order to run a specific workflow, you should create a Workflow stub:

```scala mdoc:silent
val workflowStubZIO = ZIO.serviceWithZIO[ZWorkflowClient] { workflowClient =>
  workflowClient
    .newWorkflowStub[EchoWorkflow]
    .withTaskQueue("echo-queue")
    .withWorkflowId(UUID.randomUUID().toString)
    .withWorkflowRunTimeout(10.second)
    .build
}
```

Important notes:

- You need a `ZWorkflowClient` instance to build a Workflow stub. You should either access it via ZIO environment or
  have it already created somewhere
- In `client.newWorkflowStub` method you provide the specific workflow interface you will work with
- You provide all the necessary configuration for the stub, such as the Task Queue name, the Workflow ID and the run
  timeout

Having a Workflow stub, you'll be able to execute the workflow. Executing means starting the workflow and waiting for
its completion:

```scala mdoc:silent
val workflowResultZIO = 
  for {
    echoWorkflow <- workflowStubZIO
    result <- ZWorkflowStub.execute(echoWorkflow.echo("Hello there"))
  } yield result
```

Important notes:

- Due to Java SDK implementation, it's not allowed to just invoke the workflow method directly: this will throw an
  exception
- This is because the Workflow may run on a remote machine.
- The Workflow stub is basically a proxy, which executes the Workflow via Temporal cluster
- Those, you should wrap the method invocation into a `ZWorkflowStub.execute` block

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
      _ = worker.addWorkflow[EchoWorkflow].from(new EchoWorkflowImpl)
    } yield ()
  }
}
```

Important notes:

- You need a `ZWorkerFactory` instance to create a worker. You should either access it via ZIO environment or have it
  already created somewhere
- In `worker.addWorkflow` method you provide the specific workflow interface you run in this worker
- It's required to provide the workflow implementation instance there.
- Create the workflow implementation **only inside** the `addWorkflow` method, otherwise an exception will be thrown.

## Running the Worker and the Workflow client
Let's bring all the parts into a program:

```scala mdoc:silent
val program = 
  for {
    workerFactory <-ZIO.service[ZWorkerFactory]
    workflowResult <- workerFactory.use {
      workflowResultZIO
    }
    _ <- ZIO.log(s"The workflow result: $workflowResult")
  } yield ()
```

When injecting, don't forget to provide the worker:

```scala
program.provide(
  worker,
  // Other dependencies
)
```
