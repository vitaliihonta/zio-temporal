package com.example.hello.child

import zio.temporal.*

@workflowInterface
trait GreetingWorkflow {

  /** Define the parent workflow method. This method is executed when the workflow is started. The workflow completes
    * when the workflow method finishes execution.
    */
  @workflowMethod
  def getGreeting(name: String): String
}

@workflowInterface
trait GreetingChild {

  /** Define the child workflow method. This method is executed when the workflow is started. The workflow completes
    * when the workflow method finishes execution.
    */
  @workflowMethod
  def composeGreeting(greeting: String, name: String): String

  @signalMethod
  def addPrefix(newPrefix: String): Unit
}
