package com.example.cancellation

import zio._
import zio.temporal._

@workflowInterface
trait GreetingWorkflow {
  @workflowMethod
  def getGreeting(name: String): String
}

@activityInterface
trait GreetingActivities {
  def composeGreeting(greeting: String, name: String): String
}
