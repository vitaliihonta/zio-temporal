package zio.temporal.fixture

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*
import zio.temporal.state.ZWorkflowState

@workflowInterface
trait WorkflowFooUntyped {
  @workflowMethod
  def doSomething(name: String): String
}

@workflowInterface
trait WorkflowBarUntyped {
  @workflowMethod
  def doSomethingElse(): String

  @signalMethod
  def unblock(name: String): Unit
}

class WorkflowFooUntypedImpl extends WorkflowFooUntyped {
  private val logger = ZWorkflow.makeLogger
  override def doSomething(name: String): String = {
    ZWorkflow.sleep(1.second)
    val bar = ZWorkflow.newUntypedExternalWorkflowStub(ZWorkflow.info.workflowId + "-bar")

    logger.info("Signalling external workflow...")
    bar.signal("Unblock", name)
    name + " done"
  }
}

class WorkflowBarUntypedImpl extends WorkflowBarUntyped {
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
