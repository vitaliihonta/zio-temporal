package com.example.polling.periodic

import zio.*
import zio.temporal._
import zio.temporal.workflow.*

@workflowInterface
trait PollingWorkflow {
  @workflowMethod
  def exec(): String
}

class PeriodicPollingWorkflowImpl extends PollingWorkflow {
  override def exec(): String = {
    val childWorkflow = ZWorkflow
      .newChildWorkflowStub[PollingChildWorkflow]
      .withWorkflowId(ZWorkflow.info.workflowId + "/child")
      .build

    ZChildWorkflowStub.execute(
      childWorkflow.exec(5.seconds)
    )
  }
}
