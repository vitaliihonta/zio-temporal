package zio.temporal

import zio._
import zio.temporal.activity.ZActivityOptions
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.test._
import java.util.UUID
import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.collection.mutable.ListBuffer

object AdvancedWorkflowSpec extends BaseTemporalSpec {
  override val spec: Spec[TestEnvironment with Scope, Any] =
    suite("Complex type serialization")(
      test("either") {
        ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityOptions =>
          val taskQueue  = "either-workflow"
          val workflowId = UUID.randomUUID().toString

          for {
            _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                   ZWorker.addWorkflow[EitherWorkflowImpl].fromClass @@
                   ZWorker.addActivityImplementation(ComplexTypesActivityImpl())

            _ <- ZTestWorkflowEnvironment.setup()

            stub <- ZTestWorkflowEnvironment.newWorkflowStub[EitherWorkflow](
                      ZWorkflowOptions
                        .withWorkflowId(workflowId)
                        .withTaskQueue(taskQueue)
                    )
            result <- ZWorkflowStub.execute(
                        stub.start
                      )
          } yield assertTrue(result == Right(42))
        }
      },
      test("super complex types") {
        ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityOptions =>
          val taskQueue  = "super-complex-type-workflow"
          val workflowId = UUID.randomUUID().toString

          for {
            _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                   ZWorker.addWorkflow[ComplexWorkflowImpl].fromClass @@
                   ZWorker.addActivityImplementation(ComplexTypesActivityImpl())

            _ <- ZTestWorkflowEnvironment.setup()

            stub <- ZTestWorkflowEnvironment.newWorkflowStub[ComplexWorkflow](
                      ZWorkflowOptions
                        .withWorkflowId(workflowId)
                        .withTaskQueue(taskQueue)
                    )
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
              ),
              list == List(Foo("x"), Foo("y")),
              triple == Triple(Foo("x"), 1, "y")
            )
          }
        }
      },
      test("run parameterized workflow") {
        val taskQueue = "parameterized-queue"

        def runWorkflow[Input <: ParameterizedWorkflowInput](
          stub:  ZWorkflowStub.Of[ParameterizedWorkflow[Input]]
        )(input: Input
        ): TemporalIO[List[ParameterizedWorkflowOutput]] = {
          ZIO.logInfo("Executing parameterized workflow") *>
            ZWorkflowStub.execute(stub.parentTask(input))
        }

        for {
          uuid <- ZIO.randomWith(_.nextUUID)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[SodaWorkflow].from(new SodaWorkflowImpl) @@
                 ZWorker.addWorkflow[SodaChildWorkflow].from(new SodaChildWorkflowImpl) @@
                 ZWorker.addWorkflow[JuiceWorkflow].from(new JuiceWorkflowImpl) @@
                 ZWorker.addWorkflow[JuiceChildWorkflow].from(new JuiceChildChildWorkflowImpl)

          _ <- ZTestWorkflowEnvironment.setup()

          sodaWf <- ZTestWorkflowEnvironment.newWorkflowStub[SodaWorkflow](
                      ZWorkflowOptions
                        .withWorkflowId(s"soda/$uuid")
                        .withTaskQueue(taskQueue)
                    )

          juiceWf <- ZTestWorkflowEnvironment.newWorkflowStub[JuiceWorkflow](
                       ZWorkflowOptions
                         .withWorkflowId(s"juice/$uuid")
                         .withTaskQueue(taskQueue)
                     )

          sodaResult  <- runWorkflow(sodaWf)(ParameterizedWorkflowInput.Soda("coke"))
          juiceResult <- runWorkflow(juiceWf)(ParameterizedWorkflowInput.Juice("orange"))

        } yield {
          assertTrue(
            sodaResult.sortBy(_.message) == List(
              ParameterizedWorkflowOutput("Providing with soda: coke"),
              ParameterizedWorkflowOutput("Providing with soda: coke"),
              ParameterizedWorkflowOutput("Providing with soda: coke")
            ),
            juiceResult.sortBy(_.message) == List(
              ParameterizedWorkflowOutput("Providing with orange juice (1L)"),
              ParameterizedWorkflowOutput("Providing with orange juice (2L)"),
              ParameterizedWorkflowOutput("Providing with orange juice (3L)")
            )
          )
        }
      }.provideTestWorkflowEnv,
      test("run workflow with successful sagas") {
        ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityRunOptions =>
          val taskQueue      = "saga-queue"
          val successFunc    = (_: String, _: BigDecimal) => ZIO.succeed(Done())
          val expectedResult = BigDecimal(5.0)

          for {
            workflowId <- ZIO.randomWith(_.nextUUID)

            _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                   ZWorker.addWorkflow[SagaWorkflowImpl].fromClass @@
                   ZWorker.addActivityImplementation(new TransferActivityImpl(successFunc, successFunc))
            _ <- ZTestWorkflowEnvironment.setup()
            sagaWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[SagaWorkflow](
                              ZWorkflowOptions
                                .withWorkflowId(workflowId.toString)
                                .withTaskQueue(taskQueue)
                                .withWorkflowRunTimeout(10.seconds)
                            )
            result <- ZWorkflowStub.executeWithTimeout(5.seconds)(
                        sagaWorkflow.transfer(TransferCommand("from", "to", expectedResult))
                      )
          } yield {
            assertTrue(result == expectedResult)
          }
        }

      }.provideTestWorkflowEnv,
      test("run workflow with saga compensations") {
        ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityRunOptions =>
          val taskQueue = "saga-queue"

          val From = "from"
          val To   = "to"

          val compensated = new AtomicBoolean(false)
          val withdrawn   = new AtomicInteger(0)
          val error       = TransferError("fraud")
          val withdrawFunc = (_: String, _: BigDecimal) =>
            ZIO.succeed {
              withdrawn.incrementAndGet()
              Done()
            }
          val depositFunc: (String, BigDecimal) => IO[TransferError, Done] = {
            case (From, _) =>
              ZIO.succeed {
                compensated.set(true)
                Done()
              }
            case _ =>
              ZIO.fail(error)
          }

          val amount = BigDecimal(5.0)

          for {
            workflowId <- ZIO.randomWith(_.nextUUID)

            _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                   ZWorker
                     .addWorkflow[SagaWorkflowImpl]
                     .withOptions(
                       // provide more activity options here
                       ZWorkflowImplementationOptions.default.withActivityOptions(
                         "Deposit" ->
                           ZActivityOptions
                             .withStartToCloseTimeout(5.seconds)
                             .withScheduleToCloseTimeout(60.seconds)
                             .withRetryOptions(
                               ZRetryOptions.default
                                 .withMaximumAttempts(1)
                                 .withDoNotRetry(nameOf[TransferError])
                             )
                       )
                     )
                     .fromClass @@
                   ZWorker.addActivityImplementation(new TransferActivityImpl(depositFunc, withdrawFunc))

            _ <- ZTestWorkflowEnvironment.setup()
            sagaWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[SagaWorkflow](
                              ZWorkflowOptions
                                .withWorkflowId(workflowId.toString)
                                .withTaskQueue(taskQueue)
                                .withWorkflowRunTimeout(10.seconds)
                            )
            result <- ZWorkflowStub
                        .execute(sagaWorkflow.transfer(TransferCommand(From, To, amount)))
                        .either
          } yield {
            assert(result)(Assertion.isLeft) &&
            assertTrue(
              compensated.get(),
              // activity options were applied correctly
              withdrawn.get() == 1
            )
          }
        }

      }.provideTestWorkflowEnv,
      test("run workflow with zasync") {
        val taskQueue = "zasync-queue"

        val order = new AtomicReference(ListBuffer.empty[(String, Int)])
        val fooFunc = (x: Int) => {
          println(s"foo($x)")
          order.get += ("foo" -> x)
          x
        }
        val barFunc = (x: Int) => {
          println(s"bar($x)")
          order.get += ("bar" -> x)
          x
        }

        val x = 2
        val y = 3

        def runTest(n: Int) = for {
          promiseWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[PromiseWorkflow](
                               ZWorkflowOptions
                                 .withWorkflowId(s"zasync-spec-$n")
                                 .withTaskQueue(taskQueue)
                                 .withWorkflowRunTimeout(10.seconds)
                             )
          result <- ZWorkflowStub.execute(promiseWorkflow.fooBar(x, y))
        } yield {
          assertTrue(result == x + y)
        }

        for {
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[PromiseWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new PromiseActivityImpl(fooFunc, barFunc))

          _ <- ZTestWorkflowEnvironment.setup()

          assertions <- ZIO
                          .foreach(List.range(1, 21))(runTest)
                          .map { results =>
                            val actualResult = order.get.toList

                            println(actualResult.toString)

                            results.reduce(_ && _) && assertTrue(
                              actualResult.size == 40,
                              actualResult != List.fill(20)(List("foo" -> x, "bar" -> y))
                            )
                          }
        } yield assertions
      }.provideTestWorkflowEnv @@ TestAspect.flaky(3),
      test("Runs workflows with timers") {
        val taskQueue = "payment-queue"
        for {
          workflowId <- ZIO.randomWith(_.nextUUID)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[PaymentWorkflowImpl].fromClass

          // Setup the test environment
          _ <- ZTestWorkflowEnvironment.setup()

          // Create a workflow stub
          paymentWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[PaymentWorkflow](
                               ZWorkflowOptions
                                 .withWorkflowId(workflowId.toString)
                                 .withTaskQueue(taskQueue)
                                 // Set workflow timeout
                                 .withWorkflowRunTimeout(20.minutes)
                             )

          // Start the workflow stub
          _ <- ZWorkflowStub.start(
                 paymentWorkflow.processPayment(42.1)
               )
          // Skip time
          _ <- ZTestWorkflowEnvironment.sleep(10.minutes + 30.seconds)
          // Get the workflow result
          isFinished <- paymentWorkflow.result[Boolean]
        } yield assertTrue(!isFinished)
      }.provideTestWorkflowEnv,
      test("Workflow retry") {
        val taskQueue = "retry-queue"
        for {
          workflowId <- ZIO.randomWith(_.nextUUID)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[RetryWorkflowImpl].fromClass

          _ <- ZTestWorkflowEnvironment.setup()

          retryWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[RetryWorkflow](
                             ZWorkflowOptions
                               .withWorkflowId(workflowId.toString)
                               .withTaskQueue(taskQueue)
                               .withWorkflowRunTimeout(10.second)
                           )

          result <- ZWorkflowStub.execute(retryWorkflow.withRetry(2)).either
        } yield assert(result)(Assertion.isRight(Assertion.equalTo("OK")))
      }.provideTestWorkflowEnv,
      test("Workflow memo") {
        val taskQueue = "memo-queue"
        for {
          workflowId <- ZIO.randomWith(_.nextUUID)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[MemoWorkflowImpl].fromClass

          _ <- ZTestWorkflowEnvironment.setup()

          memoWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[MemoWorkflow](
                            ZWorkflowOptions
                              .withWorkflowId(workflowId.toString)
                              .withTaskQueue(taskQueue)
                              .withMemo("zio" -> "temporal")
                              .withWorkflowRunTimeout(10.second)
                          )

          result <- ZWorkflowStub.execute(memoWorkflow.withMemo("zio"))
        } yield assert(result)(Assertion.isSome(Assertion.equalTo("temporal")))
      }.provideTestWorkflowEnv
    ).provideTestWorkflowEnv @@ TestAspect.flaky
}
