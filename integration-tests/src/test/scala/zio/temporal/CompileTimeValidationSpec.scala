package zio.temporal

import org.scalatest.wordspec.AnyWordSpec
import zio.temporal.fixture.SignalWorkflow
import zio.temporal.worker.ZWorker
import zio.temporal.workflow.{ZWorkflowClient, ZWorkflowStub}

class CompileTimeValidationSpec extends AnyWordSpec {
  private val testStub: ZWorkflowStub.Of[SignalWorkflow] = null
  private val workflowClient: ZWorkflowClient            = null
  private val worker: ZWorker                            = null

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
  }
}
