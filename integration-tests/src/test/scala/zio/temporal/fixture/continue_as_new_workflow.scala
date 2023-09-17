package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.workflow._
import scala.annotation.tailrec

@workflowInterface
trait ContinueAsNewWorkflow {
  @workflowMethod
  def doSomething(n: Int): String
}

class ContinueAsNewWorkflowImpl extends ContinueAsNewWorkflow {
  private val continueAsNewStub = ZWorkflow.newContinueAsNewStub[ContinueAsNewWorkflow]()

  override def doSomething(n: Int): String = {
    println(s"Test iteration #$n")
    if (n >= 2) "Done"
    else if (n == 1) {
      // to test compilation issues
      println(s"Continue as second n=$n")
      val stub2 = ZWorkflow.newContinueAsNewStub[ContinueAsNewWorkflow](
        ZContinueAsNewOptions.default
          .withWorkflowRunTimeout(5.minutes)
      )
      ZWorkflowContinueAsNewStub.execute(
        stub2.doSomething(n + 1)
      )
    } else {
      println(s"Continue as new n=$n")
      ZWorkflowContinueAsNewStub.execute(
        continueAsNewStub.doSomething(n + 1)
      )
    }
  }
}

@workflowInterface
trait ContinueAsNewNamedWorkflow {
  @workflowMethod(name = "ContAsNewWf")
  def doSomething(n: Int): String
}

class ContinueAsNewNamedWorkflowImpl extends ContinueAsNewNamedWorkflow {
  private val continueAsNewStub = ZWorkflow.newContinueAsNewStub[ContinueAsNewNamedWorkflow]()

  override def doSomething(n: Int): String = {
    println(s"Test iteration #$n")
    if (n >= 2) "Done"
    else if (n == 1) {
      // to test compilation issues
      println(s"Continue as second n=$n")
      val stub2 = ZWorkflow.newContinueAsNewStub[ContinueAsNewNamedWorkflow]()
      ZWorkflowContinueAsNewStub.execute(
        stub2.doSomething(n + 1)
      )
    } else {
      println(s"Continue as new n=$n")
      ZWorkflowContinueAsNewStub.execute(
        continueAsNewStub.doSomething(n + 1)
      )
    }
  }
}
