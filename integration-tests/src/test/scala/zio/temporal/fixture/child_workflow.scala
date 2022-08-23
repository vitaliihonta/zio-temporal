package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.activity._

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
    val child = ZWorkflow.newChildWorkflowStub[GreetingChild].build

    println("Invoking child workflow...")
    val greetingPromise = ZWorkflowStub.async(child.composeGreeting("Hello", name))
    println("Child workflow started!")
    greetingPromise.run.value
  }
}

class GreetingChildImpl extends GreetingChild {
  override def composeGreeting(greeting: String, name: String): String = {
    println(s"composeGreeting($greeting, $name)")
    s"$greeting $name!"
  }
}
