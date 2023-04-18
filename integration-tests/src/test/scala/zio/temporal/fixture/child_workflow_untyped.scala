package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.activity._

@workflowInterface
trait GreetingUntypedWorkflow {
  @workflowMethod
  def getGreeting(name: String): String
}

@workflowInterface
trait GreetingUntypedChild {
  @workflowMethod
  def composeGreeting(greeting: String, name: String): String
}

class GreetingUntypedWorkflowImpl extends GreetingUntypedWorkflow {
  override def getGreeting(name: String): String = {
    val child = ZWorkflow.newUntypedChildWorkflowStub("GreetingUntypedChild").build

    println("Invoking untyped child workflow...")
    val greetingPromise = child.executeAsync[String]("Hello", name)
    println("Child untyped workflow started!")
    greetingPromise.run.getOrThrow
  }
}

class GreetingUntypedChildImpl extends GreetingUntypedChild {
  override def composeGreeting(greeting: String, name: String): String = {
    println(s"untyped composeGreeting($greeting, $name)")
    s"$greeting $name!"
  }
}
