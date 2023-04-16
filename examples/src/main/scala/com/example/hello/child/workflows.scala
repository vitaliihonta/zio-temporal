package com.example.hello.child

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

class GreetingChildImpl extends GreetingChild {
  private val logger = ZWorkflow.getLogger(getClass)
  override def composeGreeting(greeting: String, name: String): String = {
    logger.info("Composing greeting...")
    ZWorkflow.sleep(3.seconds)
    logger.info("Composing greeting done!")
    s"$greeting $name!"
  }
}

// Define the parent workflow implementation. It implements the getGreeting workflow method
class GreetingWorkflowImpl extends GreetingWorkflow {
  private val logger = ZWorkflow.getLogger(getClass)
  override def getGreeting(name: String): String = {
    /*
     * Define the child workflow stub. Since workflows are stateful,
     * a new stub must be created for each child workflow.
     */
    val child =
      io.temporal.workflow.Workflow.newUntypedChildWorkflowStub(
        simpleNameOf[GreetingChild],
        io.temporal.workflow.ChildWorkflowOptions
          .newBuilder()
          .build()
      )

    // This is a non blocking call that returns immediately.
    // Use child.composeGreeting("Hello", name) to call synchronously

    logger.info("Starting child workflow...")
    /*
     * Invoke the child workflows composeGreeting workflow method.
     * This call is non-blocking and returns immediately returning a {@link io.temporal.workflow.Promise}
     *
     * You can use child.composeGreeting("Hello", name) instead to call the child workflow method synchronously.
     */
    val greeting = ZAsync.fromJava(
      child.executeAsync(classOf[String], "Hello", name)
    )
    logger.info("Waiting until child workflow completes...")
    // Wait for the child workflow to complete and return its results
    greeting.run.getOrThrow
  }
}
