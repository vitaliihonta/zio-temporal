package com.example.schedule

import zio.temporal._
import zio.temporal.workflow.ZWorkflow

@workflowInterface
trait HelloWorkflowWithTime {

  @workflowMethod
  def printGreeting(name: String): Unit
}

class HelloWorkflowWithTimeImpl extends HelloWorkflowWithTime {
  private val logger = ZWorkflow.makeLogger

  override def printGreeting(name: String): Unit = {
    logger.info(s"Hello $name, it's ${ZWorkflow.currentTimeMillis.toLocalDateTime()} now")
  }
}
