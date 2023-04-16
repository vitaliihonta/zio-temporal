package com.example.hello.cancellation

import zio.*
import zio.temporal.*

@workflowInterface
trait GreetingWorkflow {
  @workflowMethod
  def getGreeting(name: String): String
}

@activityInterface
trait GreetingActivities {
  def composeGreeting(greeting: String, name: String): String
}
