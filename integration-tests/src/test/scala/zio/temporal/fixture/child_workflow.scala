package zio.temporal.fixture

import zio.temporal._
import zio.temporal.workflow._

@workflowInterface
trait GreetingWorkflow {
  @workflowMethod
  def getGreeting(name: String): String
}

@workflowInterface
trait GreetingChild {
  @workflowMethod
  def composeGreeting(greeting: String, name: String): String
}

class GreetingWorkflowImpl extends GreetingWorkflow {
  override def getGreeting(name: String): String = {
    val child = ZWorkflow.newChildWorkflowStub[GreetingChild](
      ZChildWorkflowOptions
        .withWorkflowId(s"greeting-child/${ZWorkflow.info.workflowId}")
    )

    println("Invoking child workflow...")
    val greetingPromise = ZChildWorkflowStub.executeAsync(
      child.composeGreeting("Hello", name)
    )
    println("Child workflow started!")
    greetingPromise.run.getOrThrow
  }
}

class GreetingChildImpl extends GreetingChild {
  override def composeGreeting(greeting: String, name: String): String = {
    println(s"composeGreeting($greeting, $name)")
    s"$greeting $name!"
  }
}
