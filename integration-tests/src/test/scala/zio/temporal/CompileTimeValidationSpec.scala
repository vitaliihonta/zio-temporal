package zio.temporal

import org.scalatest.wordspec.AnyWordSpec
import zio.temporal.activity.ZActivityStub
import zio.temporal.fixture.{SignalWorkflow, ZioActivity}
import zio.temporal.worker.ZWorker
import zio.temporal.workflow.{
  ZChildWorkflowStub,
  ZExternalWorkflowStub,
  ZWorkflowClient,
  ZWorkflowContinueAsNewStub,
  ZWorkflowStub
}
import zio._

class CompileTimeValidationSpec extends AnyWordSpec {
  private val testStub: ZWorkflowStub.Of[SignalWorkflow] = null
  private val workflowClient: ZWorkflowClient            = null
  private val worker: ZWorker                            = null

  private val wf: SignalWorkflow = null
  private val act: ZioActivity   = null

  @workflowInterface
  trait SampleWorkflow
  class SampleWorkflowImpl private () extends SampleWorkflow

  "zio-temporal" should {
    "not allow to invoke non-workflow methods in execute" in {
      assertDoesNotCompile(
        """ZWorkflowStub.execute(testStub.hashCode())"""
      )
    }

    "not allow to invoke non-workflow methods in start" in {
      assertDoesNotCompile(
        """ZWorkflowStub.start(testStub.hashCode())"""
      )
    }

    "not allow to invoke non-query methods in query" in {
      assertDoesNotCompile(
        """ZWorkflowStub.query(testStub.toString)"""
      )
    }

    "not allow to invoke non-signal methods in signal" in {
      assertDoesNotCompile(
        """ZWorkflowStub.signal(testStub.hashCode())"""
      )
    }

    "not allow to invoke non-signal methods in signalWith" in {
      assertDoesNotCompile(
        """
          workflowClient
            .signalWith(testStub.hashCode())
            .start(testStub.echoServer("Hello!"))
          """
      )
    }

    "not allow to invoke non-signal methods in signalWith(...).start" in {
      assertDoesNotCompile(
        """
          workflowClient
            .signalWith(testStub.echo("Foo"))
            .start(testStub.hashCode())
          """
      )
    }

    "not allow creating workers with abstract workflow types" in {
      assertDoesNotCompile(
        """
          worker.addWorkflow[SampleWorkflow].fromClass
          """
      )
    }

    "not allow creating workers with non-workflow types" in {
      assertDoesNotCompile(
        """
          worker.addWorkflow[String]
          """
      )
    }

    "not allow creating workers with workflows without zero-argument constructor" in {
      assertDoesNotCompile(
        """
          worker.addWorkflow[SampleWorkflowImpl].fromClass
          """
      )
    }

    "not allow creating workers with non-activities" in {
      assertDoesNotCompile(
        """
          worker.addActivityImplementation("fooo")
          """
      )
    }

    "not allow using non-workflows in ZWorkflowStub.xxx methods" in {
      assertDoesNotCompile(
        """
        ZWorkflowStub.execute(
          wf.echoServer("hello")
        )
        """
      )

      assertDoesNotCompile(
        """
        ZWorkflowStub.executeWithTimeout(5.seconds)(
          wf.echoServer("hello")
        )
        """
      )

      assertDoesNotCompile(
        """
          ZWorkflowStub.signal(
            wf.echo("hello")
          )
        """
      )

      assertDoesNotCompile(
        """
          ZWorkflowStub.query(
            wf.getProgress(None)
          )
          """
      )
    }

    "not allow using non-workflows in ZWorkflowContinueAsNewStub.xxx methods" in {
      assertDoesNotCompile(
        """
        ZWorkflowContinueAsNewStub.execute(
          wf.echoServer("hello 2")
        )
      """
      )
    }

    "not allow using non-workflows in ZExternalWorkflowStub.xxx methods" in {
      assertDoesNotCompile(
        """
        ZExternalWorkflowStub.signal(
          wf.echo("hello 2")
        )
      """
      )
    }

    "not allow using non-activities in ZActivityStub.xxx methods" in {
      assertDoesNotCompile(
        """
          ZActivityStub.execute(
            act.echo("hello")
          )
        """
      )

      assertDoesNotCompile(
        """
          ZActivityStub.executeAsync(
            act.echo("hello 2")
          )
        """
      )
    }
  }
}
