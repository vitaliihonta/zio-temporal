package com.example.child

import zio._
import zio.temporal._
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow._

class GreetingChildImpl extends GreetingChild {
  private val logger = ZWorkflow.makeLogger
  private val prefix = ZWorkflowState.empty[String]

  override def composeGreeting(greeting: String, name: String): String = {
    logger.info("Composing greeting...")
    ZWorkflow.sleep(3.seconds)
    logger.info("Composing greeting done!")
    s"$greeting $name!" + " " + prefix.snapshotOrElse("")
  }

  override def addPrefix(newPrefix: String): Unit = {
    logger.info(s"Prefix set: $newPrefix")
    prefix := newPrefix
  }
}

// Define the parent workflow implementation. It implements the getGreeting workflow method
class GreetingWorkflowImpl extends GreetingWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def getGreeting(name: String): String = {
    /*
     * Define the child workflow stub. Since workflows are stateful,
     * a new stub must be created for each child workflow.
     */
    val child: ZChildWorkflowStub.Of[GreetingChild] =
      ZWorkflow.newChildWorkflowStub[GreetingChild](
        ZChildWorkflowOptions.withWorkflowId(s"greeting-child/${ZWorkflow.info.workflowId}")
      )

    // This is a non blocking call that returns immediately.
    // Use child.composeGreeting("Hello", name) to call synchronously

    logger.info("Starting child workflow...")
    /*
     * Invoke the child workflows composeGreeting workflow method.
     * This call is non-blocking and returns immediately returning a {@link io.temporal.workflow.Promise}
     *
     * You can use ZChildWorkflowStub.execute(child.composeGreeting("Hello", name))
     * instead to call the child workflow method synchronously.
     */
    val greeting: ZAsync[String] = ZChildWorkflowStub.executeAsync(
      child.composeGreeting("Hello", name)
    )
    logger.info("Waiting until child workflow completes...")

    // Try commenting/uncommenting out the signal
    ZChildWorkflowStub.signal(
      child.addPrefix("How are you doing?")
    )

    // Wait for the child workflow to complete and return its results
    greeting.run.getOrThrow
  }
}
