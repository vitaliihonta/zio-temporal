package zio.temporal

import zio._
import zio.test._
import zio.logging.backend.SLF4J
import zio.temporal.activity.{ZActivityImplementationObject, ZActivityOptions, ZActivityRunOptions}
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.test._

import java.util.concurrent.atomic.AtomicInteger

object WorkerSpec extends BaseTemporalSpec {
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    testEnvironment ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val spec = suite("ZWorker")(
    test("Register workflows using addWorkflowImplementations") {
      val taskQueue = "child-2-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflowImplementations(
                 ZWorkflowImplementationClass[GreetingWorkflowImpl],
                 ZWorkflowImplementationClass[GreetingChildImpl]
               )
        _ <- ZTestWorkflowEnvironment.setup()
        greetingWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[GreetingWorkflow](
                              ZWorkflowOptions
                                .withWorkflowId(workflowId.toString)
                                .withTaskQueue(taskQueue)
                                .withWorkflowRunTimeout(10.second)
                            )
        result <- ZWorkflowStub.execute(
                    greetingWorkflow.getGreeting("Vitalii")
                  )
      } yield assertTrue(result == "Hello Vitalii!")
    }.provideTestWorkflowEnv,
    test("Register activities using addActivityImplementations") {
      val taskQueue = "multi-activities-workflow-queue"

      ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityRunOptions =>
        for {
          workflowId <- ZIO.randomWith(_.nextUUID)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflowImplementations(
                   List(
                     ZWorkflowImplementationClass[MultiActivitiesWorkflowImpl]
                   )
                 ) @@
                 ZWorker.addActivityImplementations(
                   ZActivityImplementationObject(new ZioActivityImpl()),
                   ZActivityImplementationObject(ComplexTypesActivityImpl())
                 )
          _ <- ZTestWorkflowEnvironment.setup()
          multiWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[MultiActivitiesWorkflow](
                             ZWorkflowOptions
                               .withWorkflowId(workflowId.toString)
                               .withTaskQueue(taskQueue)
                               .withWorkflowRunTimeout(10.second)
                           )
          result <- ZWorkflowStub.execute(
                      multiWorkflow.doSomething("Vitalii")
                    )
        } yield assertTrue(result == "Echoed Vitalii, list=2")
      }
    }.provideTestWorkflowEnv,
    test("Register activities using addActivityImplementationsLayer") {
      val taskQueue = "multi-activities-workflow-queue"

      val activitiesLayer
        : URLayer[ReporterService with ZActivityRunOptions[Any], List[ZActivityImplementationObject[_]]] =
        ZLayer.collectAll(
          List(
            ZActivityImplementationObject.layer(
              ZLayer.fromFunction(new ZioActivityImpl()(_: ZActivityRunOptions[Any]))
            ),
            ZActivityImplementationObject.layer(
              ZLayer.fromFunction(ComplexTypesActivityImpl()(_: ZActivityRunOptions[Any]))
            ),
            ZActivityImplementationObject.layer(ActivityWithDependenciesImpl.make)
          )
        )

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflowImplementations(
                 List(
                   ZWorkflowImplementationClass[EvenMoreMultiActivitiesWorkflowImpl]
                 )
               ) @@
               ZWorker.addActivityImplementationsLayer(activitiesLayer)
        _ <- ZTestWorkflowEnvironment.setup()
        multiWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[MultiActivitiesWorkflow](
                           ZWorkflowOptions
                             .withWorkflowId(workflowId.toString)
                             .withTaskQueue(taskQueue)
                             .withWorkflowRunTimeout(10.second)
                         )
        result <- ZWorkflowStub.execute(
                    multiWorkflow.doSomething("Vitalii")
                  )
      } yield assertTrue(result == "Echoed Vitalii, list=2")
    }.provideSome[Scope](
      ZTestWorkflowEnvironment.makeDefault[Any],
      ZLayer.fromZIO(ZTestWorkflowEnvironment.activityRunOptions[Any]),
      ReporterService.make
    ) @@ TestAspect.withLiveClock
  )
}
