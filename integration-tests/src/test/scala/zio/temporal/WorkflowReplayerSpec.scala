package zio.temporal

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.test._

object WorkflowReplayerSpec extends BaseTemporalSpec {
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    testEnvironment ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val spec = suite("ZWorkflowReplayer")(
    test("Replays successful workflow run from resource") {
      for {
        worker <- ZTestWorkflowEnvironment.newWorker("replay-test-queue") @@
                    ZWorker.addWorkflow[SagaWorkflowImpl].fromClass

        _ <- ZTestWorkflowEnvironment.setup()

        _ <- ZWorkflowReplayer.replayWorkflowExecutionFromResource(
               "replay_saga_workflow_success.json",
               worker
             )
      } yield {
        assertTrue(1 == 1)
      }
    }.provideTestWorkflowEnv,
    test("Replays failed workflow run from resource") {
      ZWorkflowReplayer
        .replayWorkflowExecutionFromResource(
          "replay_saga_workflow_failed.json",
          ZWorkflowImplementationClass[SagaWorkflowImpl]
        )
        .as {
          assertTrue(1 == 1)
        }
    }.provideTestWorkflowEnv
  )
}
