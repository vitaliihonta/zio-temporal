package zio.temporal.fixture

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*
import zio.temporal.state.ZWorkflowState

@workflowInterface
trait WorkflowFoo {
  @workflowMethod
  def doSomething(name: String): String
}

@workflowInterface
trait WorkflowBar {
  @workflowMethod
  def doSomethingElse(): String

  @signalMethod
  def unblock(name: String): Unit
}

class WorkflowFooImpl extends WorkflowFoo {
  private val logger = ZWorkflow.makeLogger
  override def doSomething(name: String): String = {
    ZWorkflow.sleep(1.second)
    val bar = ZWorkflow.newExternalWorkflowStub[WorkflowBar](ZWorkflow.info.workflowId + "-bar")
    logger.info("Signalling external workflow...")
    ZExternalWorkflowStub.signal(
      bar.unblock(name)
    )
    name + " done"
  }
}

class WorkflowBarImpl extends WorkflowBar {
  private val logger = ZWorkflow.makeLogger

  private val state = ZWorkflowState.empty[String]

  override def doSomethingElse(): String = {
    logger.info("Waiting until unblocked...")
    ZWorkflow.awaitWhile(state.isEmpty)
    state.snapshot
  }

  override def unblock(name: String): Unit = {
    logger.info(s"Unblocked with name=$name")
    state := name
  }
}
