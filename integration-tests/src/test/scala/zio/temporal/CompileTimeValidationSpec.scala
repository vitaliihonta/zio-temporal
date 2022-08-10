package zio.temporal

import org.scalatest.wordspec.AnyWordSpec
import zio.temporal.fixture.SignalWorkflow
import zio.temporal.workflow.{ZWorkflowClient, ZWorkflowStub}

class CompileTimeValidationSpec extends AnyWordSpec {
  private val testStub: ZWorkflowStub.Of[SignalWorkflow] = null
  private val workflowClient: ZWorkflowClient            = null

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
  }
}
