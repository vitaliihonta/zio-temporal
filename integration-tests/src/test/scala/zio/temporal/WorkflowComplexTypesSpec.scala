package zio.temporal

import zio._
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.temporal.worker.ZWorker
import zio.temporal.workflow.ZWorkflowStub
import zio.test._
import java.util.UUID

object WorkflowComplexTypesSpec extends ZIOSpecDefault {
  override def spec: Spec[TestEnvironment with Scope, Any] =
    suite("Complex type serialization")(
      test("either") {
        ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
          val taskQueue  = "either-workflow"
          val workflowId = UUID.randomUUID().toString

          for {
            _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                   ZWorker.addWorkflow[EitherWorkflowImpl].fromClass @@
                   ZWorker.addActivityImplementation(ComplexTypesActivityImpl())

            _ <- ZTestWorkflowEnvironment.setup()

            stub <- ZTestWorkflowEnvironment
                      .newWorkflowStub[EitherWorkflow]
                      .withTaskQueue(taskQueue)
                      .withWorkflowId(workflowId)
                      .build
            result <- ZWorkflowStub.execute(
                        stub.start
                      )
          } yield assertTrue(result == Right(42))
        }
      },
      test("super complex types") {
        ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
          val taskQueue  = "super-complex-type-workflow"
          val workflowId = UUID.randomUUID().toString

          for {
            _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                   ZWorker.addWorkflow[ComplexWorkflowImpl].fromClass @@
                   ZWorker.addActivityImplementation(ComplexTypesActivityImpl())

            _ <- ZTestWorkflowEnvironment.setup()

            stub <- ZTestWorkflowEnvironment
                      .newWorkflowStub[ComplexWorkflow]
                      .withTaskQueue(taskQueue)
                      .withWorkflowId(workflowId)
                      .build
            _ <- ZWorkflowStub.start(
                   stub.start
                 )
            // give time for activities to execute
            _ <- ZIO.sleep(2.seconds)
            list <- ZWorkflowStub.query(
                      stub.query1
                    )
            triple <- ZWorkflowStub.query(
                        stub.query2
                      )
            // resume the workflow execution
            _ <- ZWorkflowStub.signal(
                   stub.resume()
                 )
            result <- stub.result[Either[List[String], Triple[Option[Int], Set[UUID], Boolean]]]
          } yield {
            assertTrue(
              result == Right(
                Triple(
                  first = Option.empty[Int],
                  second = Set(
                    UUID.fromString("8858bba1-3193-4c68-8e0e-5e29caeac210"),
                    UUID.fromString("992858ce-58d1-4887-a7a3-6d3ee2f62c09")
                  ),
                  third = false
                )
              )
            ) &&
            assertTrue(list == List(Foo("x"), Foo("y"))) &&
            assertTrue(triple == Triple(Foo("x"), 1, "y"))
          }
        }
      }
    ).provideEnv @@ TestAspect.flaky @@ TestAspect.sequential

  private implicit class ProvidedTestkit[E, A](thunk: Spec[ZTestWorkflowEnvironment[Any] with Scope, E]) {
    def provideEnv: Spec[Scope, E] =
      thunk.provideSome[Scope](
        ZTestEnvironmentOptions.default,
        ZTestWorkflowEnvironment.make[Any]
      ) @@ TestAspect.withLiveClock
  }
}
