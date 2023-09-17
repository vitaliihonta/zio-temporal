package zio.temporal

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.activity.{ZActivityImplementationObject, ZActivityOptions, ZActivityType}
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.test._

import java.util.concurrent.atomic.{AtomicBoolean, AtomicInteger, AtomicReference}
import scala.collection.mutable.ListBuffer

object WorkflowSpec extends BaseTemporalSpec {
  override val bootstrap: ZLayer[Any, Any, TestEnvironment] =
    testEnvironment ++ Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val spec = suite("ZWorkflow")(
    test("runs simple workflow") {
      val taskQueue = "sample-queue"
      val sampleIn  = "Fooo"
      val sampleOut = sampleIn

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SampleWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        sampleWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[SampleWorkflow](
                            ZWorkflowOptions
                              .withWorkflowId(workflowId.toString)
                              .withTaskQueue(taskQueue)
                              .withWorkflowRunTimeout(10.second)
                          )
        result <- ZWorkflowStub.execute(sampleWorkflow.echo(sampleIn))
      } yield assertTrue(result == sampleOut)

    }.provideTestWorkflowEnv,
    test("runs simple workflow with custom name") {
      val taskQueue = "sample-named-queue"
      val sampleIn  = "Fooo"
      val sampleOut = sampleIn

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SampleNamedWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        sampleWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[SampleNamedWorkflow](
                            ZWorkflowOptions
                              .withWorkflowId(workflowId.toString)
                              .withTaskQueue(taskQueue)
                              .withWorkflowRunTimeout(10.second)
                          )
        result <- ZWorkflowStub.execute(sampleWorkflow.echo(sampleIn))
      } yield assertTrue(result == sampleOut)

    }.provideTestWorkflowEnv,
    test("runs child workflows") {
      val taskQueue = "child-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[GreetingWorkflowImpl].fromClass @@
               ZWorker.addWorkflow[GreetingChildImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        greetingWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[GreetingWorkflow](
                              ZWorkflowOptions
                                .withWorkflowId(workflowId.toString)
                                .withTaskQueue(taskQueue)
                                .withWorkflowRunTimeout(10.second)
                            )
        result <- ZWorkflowStub.execute(greetingWorkflow.getGreeting("Vitalii"))
      } yield assertTrue(result == "Hello Vitalii!")

    }.provideTestWorkflowEnv,
    test("runs child workflows with custom names") {
      val taskQueue = "child-named-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[GreetingWorkflowNamedImpl].fromClass @@
               ZWorker.addWorkflow[GreetingNamedChildImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        greetingWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[GreetingNamedWorkflow](
                              ZWorkflowOptions
                                .withWorkflowId(workflowId.toString)
                                .withTaskQueue(taskQueue)
                                .withWorkflowRunTimeout(10.second)
                            )
        result <- ZWorkflowStub.execute(greetingWorkflow.getGreeting("Vitalii"))
      } yield assertTrue(result == "Hello Vitalii!")

    }.provideTestWorkflowEnv,
    test("runs child untyped workflows") {
      val taskQueue = "child-untyped-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[GreetingUntypedWorkflowImpl].fromClass @@
               ZWorker.addWorkflow[GreetingUntypedChildImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        greetingWorkflow <- ZTestWorkflowEnvironment.newUntypedWorkflowStub(
                              workflowType = "GreetingUntypedWorkflow",
                              options = ZWorkflowOptions
                                .withWorkflowId(workflowId.toString)
                                .withTaskQueue(taskQueue)
                                .withWorkflowRunTimeout(10.second)
                            )
        result <- greetingWorkflow.execute[String]("Vitalii")
      } yield assertTrue(result == "Hello Vitalii!")

    }.provideTestWorkflowEnv,
    test("runs workflows with external workflow signalling") {
      val taskQueue      = "external-workflow-queue"
      val input          = "Wooork"
      val expectedOutput = "Wooork done"
      for {
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[WorkflowFooImpl].fromClass @@
               ZWorker.addWorkflow[WorkflowBarImpl].fromClass

        _             <- ZTestWorkflowEnvironment.setup()
        fooWorkflowId <- ZIO.randomWith(_.nextUUID).map(_.toString)
        fooWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[WorkflowFoo](
                         ZWorkflowOptions
                           .withWorkflowId(fooWorkflowId)
                           .withTaskQueue(taskQueue)
                           .withWorkflowRunTimeout(10.second)
                       )
        barWorkflowId = fooWorkflowId + "-bar"
        barWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[WorkflowBar](
                         ZWorkflowOptions
                           .withWorkflowId(barWorkflowId)
                           .withTaskQueue(taskQueue)
                           .withWorkflowRunTimeout(10.second)
                       )
        _ <- ZIO.collectAllParDiscard(
               List(
                 ZWorkflowStub.start(
                   fooWorkflow.doSomething(input)
                 ),
                 ZWorkflowStub.start(
                   barWorkflow.doSomethingElse()
                 )
               )
             )
        result <- fooWorkflow.result[String]
      } yield assertTrue(result == expectedOutput)

    }.provideTestWorkflowEnv,
    test("runs workflows with untyped external workflow signalling") {
      val taskQueue      = "external-untyped-workflow-queue"
      val input          = "Wooork"
      val expectedOutput = "Wooork done"
      for {
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[WorkflowFooUntypedImpl].fromClass @@
               ZWorker.addWorkflow[WorkflowBarUntypedImpl].fromClass

        _             <- ZTestWorkflowEnvironment.setup()
        fooWorkflowId <- ZIO.randomWith(_.nextUUID).map(_.toString)
        fooWorkflow <- ZTestWorkflowEnvironment.newUntypedWorkflowStub(
                         workflowType = "WorkflowFooUntyped",
                         options = ZWorkflowOptions
                           .withWorkflowId(fooWorkflowId)
                           .withTaskQueue(taskQueue)
                           .withWorkflowRunTimeout(10.second)
                       )
        barWorkflowId = fooWorkflowId + "-bar"
        barWorkflow <- ZTestWorkflowEnvironment.newUntypedWorkflowStub(
                         workflowType = "WorkflowBarUntyped",
                         options = ZWorkflowOptions
                           .withWorkflowId(barWorkflowId)
                           .withTaskQueue(taskQueue)
                           .withWorkflowRunTimeout(10.second)
                       )
        _ <- ZIO.collectAllParDiscard(
               List(
                 fooWorkflow.start(input),
                 barWorkflow.start()
               )
             )
        result <- fooWorkflow.result[String](timeout = 5.seconds)
      } yield assertTrue(result contains expectedOutput)

    }.provideTestWorkflowEnv,
    test("run workflow with signals") {
      val taskQueue = "signal-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SignalWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        signalWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[SignalWorkflow](
                            ZWorkflowOptions
                              .withWorkflowId(workflowId)
                              .withTaskQueue(taskQueue)
                              .withWorkflowRunTimeout(30.seconds)
                              .withWorkflowTaskTimeout(30.seconds)
                              .withWorkflowExecutionTimeout(30.seconds)
                          )
        _ <- ZIO.log("Before start")
        _ <- ZWorkflowStub.start(
               signalWorkflow.echoServer("ECHO")
             )
        _            <- ZIO.log("Started")
        workflowStub <- ZTestWorkflowEnvironment.newWorkflowStub[SignalWorkflow](workflowId)
        _            <- ZIO.log("New stub created!")
        progress <- ZWorkflowStub.query(
                      workflowStub.getProgress(default = None)
                    )
        _               <- ZIO.log(s"Progress=$progress")
        progressDefault <- ZWorkflowStub.query(workflowStub.getProgress(default = Some("default")))
        _               <- ZIO.log(s"Progress_default=$progressDefault")
        _ <- ZWorkflowStub.signal(
               workflowStub.echo("Hello!")
             )
        progress2 <- ZWorkflowStub
                       .query(workflowStub.getProgress(default = None))
                       .repeatWhile(_.isEmpty)
        _      <- ZIO.log(s"Progress2=$progress2")
        result <- workflowStub.result[String]
      } yield {
        assertTrue(
          progress.isEmpty,
          progressDefault.contains("default"),
          progress2.contains("Hello!"),
          result == "ECHO Hello!"
        )
      }

    }.provideTestWorkflowEnv,
    test("run workflow with untyped signals") {
      val taskQueue = "signal-untyped-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SignalWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        signalWorkflow <- ZTestWorkflowEnvironment.newUntypedWorkflowStub(
                            workflowType = "SignalWorkflow",
                            options = ZWorkflowOptions
                              .withWorkflowId(workflowId)
                              .withTaskQueue(taskQueue)
                              .withWorkflowRunTimeout(30.seconds)
                              .withWorkflowTaskTimeout(30.seconds)
                              .withWorkflowExecutionTimeout(30.seconds)
                          )
        _               <- ZIO.log("Before start")
        _               <- signalWorkflow.start("ECHO")
        _               <- ZIO.log("Started")
        workflowStub    <- ZTestWorkflowEnvironment.newUntypedWorkflowStub(workflowId, runId = None)
        _               <- ZIO.log("New stub created!")
        progress        <- workflowStub.query[Option[String]]("progress", None)
        _               <- ZIO.log(s"Progress=$progress")
        progressDefault <- workflowStub.query[Option[String]]("progress", Some("default"))
        _               <- ZIO.log(s"Progress_default=$progressDefault")
        _               <- workflowStub.signal("echo", "Hello!")

        progress2 <- workflowStub
                       .query[Option[String]]("progress", None)
                       .repeatWhile(_.isEmpty)
        _      <- ZIO.log(s"Progress2=$progress2")
        result <- workflowStub.result[String]
      } yield {
        assertTrue(
          progress.isEmpty,
          progressDefault.contains("default"),
          progress2.contains("Hello!"),
          result == "ECHO Hello!"
        )
      }

    }.provideTestWorkflowEnv,
    test("run workflow with ZIO") {
      ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityRunOptions =>
        val taskQueue = "zio-queue"

        for {
          workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ZioWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new ZioActivityImpl())
          _ <- ZTestWorkflowEnvironment.setup()
          zioWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[ZioWorkflow](
                           ZWorkflowOptions
                             .withWorkflowId(workflowId)
                             .withTaskQueue(taskQueue)
                             .withWorkflowRunTimeout(30.seconds)
                             .withWorkflowTaskTimeout(30.seconds)
                             .withWorkflowExecutionTimeout(30.seconds)
                         )
          _ <- ZWorkflowStub.start(
                 zioWorkflow.echo("HELLO THERE")
               )
          workflowStub <- ZTestWorkflowEnvironment.newWorkflowStub[ZioWorkflow](workflowId)
          _ <- ZWorkflowStub.signal(
                 workflowStub.complete()
               )
          result <- workflowStub.result[String]
        } yield assertTrue(result == "Echoed HELLO THERE")
      }

    }.provideTestWorkflowEnv,
    test("run workflow with ZIO with local activity") {
      ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityRunOptions =>
        val taskQueue = "zio-local-queue"

        for {
          workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ZioLocalWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new ZioActivityImpl())
          _ <- ZTestWorkflowEnvironment.setup()
          zioWorkflow <- ZTestWorkflowEnvironment.newWorkflowStub[ZioLocalWorkflow](
                           ZWorkflowOptions
                             .withWorkflowId(workflowId)
                             .withTaskQueue(taskQueue)
                             .withWorkflowRunTimeout(30.seconds)
                             .withWorkflowTaskTimeout(30.seconds)
                             .withWorkflowExecutionTimeout(30.seconds)
                         )
          _ <- ZWorkflowStub.start(
                 zioWorkflow.echo("HELLO THERE")
               )
          workflowStub <- ZTestWorkflowEnvironment.newWorkflowStub[ZioLocalWorkflow](workflowId)
          _ <- ZWorkflowStub.signal(
                 workflowStub.complete()
               )
          result <- workflowStub.result[String]
        } yield assertTrue(result == "Echoed HELLO THERE")
      }

    }.provideTestWorkflowEnv,
    test("run workflow with continue as new") {
      val taskQueue = "continue-as-new-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)

        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[ContinueAsNewWorkflowImpl].fromClass

        _ <- ZTestWorkflowEnvironment.setup()
        workflow <- ZTestWorkflowEnvironment.newWorkflowStub[ContinueAsNewWorkflow](
                      ZWorkflowOptions
                        .withWorkflowId(workflowId)
                        .withTaskQueue(taskQueue)
                        .withWorkflowRunTimeout(30.seconds)
                        .withWorkflowTaskTimeout(30.seconds)
                        .withWorkflowExecutionTimeout(30.seconds)
                    )
        result <- ZWorkflowStub.execute(
                    workflow.doSomething(0)
                  )
      } yield assertTrue(result == "Done")

    }.provideTestWorkflowEnv,
    test("run custom-named workflow with continue as new") {
      val taskQueue = "continue-as-new-named-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)

        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[ContinueAsNewNamedWorkflowImpl].fromClass

        _ <- ZTestWorkflowEnvironment.setup()
        workflow <- ZTestWorkflowEnvironment.newWorkflowStub[ContinueAsNewNamedWorkflow](
                      ZWorkflowOptions
                        .withWorkflowId(workflowId)
                        .withTaskQueue(taskQueue)
                        .withWorkflowRunTimeout(30.seconds)
                        .withWorkflowTaskTimeout(30.seconds)
                        .withWorkflowExecutionTimeout(30.seconds)
                    )
        result <- ZWorkflowStub.execute(
                    workflow.doSomething(0)
                  )
      } yield assertTrue(result == "Done")

    }.provideTestWorkflowEnv,
    test("run workflow with ZIO untyped activity") {
      ZTestWorkflowEnvironment.activityRunOptionsWithZIO[Any] { implicit activityRunOptions =>
        val taskQueue = "zio-untyped-queue"

        for {
          workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)

          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ZioWorkflowUntypedImpl].fromClass @@
                 ZWorker.addActivityImplementation(new ZioUntypedActivityImpl())
          _ <- ZTestWorkflowEnvironment.setup()
          zioWorkflow <- ZTestWorkflowEnvironment.newUntypedWorkflowStub(
                           workflowType = "ZioWorkflowUntyped",
                           options = ZWorkflowOptions
                             .withWorkflowId(workflowId)
                             .withTaskQueue(taskQueue)
                             .withWorkflowRunTimeout(30.seconds)
                             .withWorkflowTaskTimeout(30.seconds)
                             .withWorkflowExecutionTimeout(30.seconds)
                         )
          _      <- zioWorkflow.start("HELLO THERE")
          _      <- zioWorkflow.signal("complete")
          result <- zioWorkflow.result[String]
        } yield assertTrue(result == "Echoed HELLO THERE")
      }

    }.provideTestWorkflowEnv,
    test("run workflow with signal and start") {
      val taskQueue = "signal-with-start-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID)

        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SignalWithStartWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        workflowStub <- ZTestWorkflowEnvironment.newWorkflowStub[SignalWithStartWorkflow](
                          ZWorkflowOptions
                            .withWorkflowId(workflowId.toString)
                            .withTaskQueue(taskQueue)
                            .withWorkflowRunTimeout(10.seconds)
                        )
        _ <- workflowStub.signalWithStart(
               workflowStub.echoServer(),
               workflowStub.echo("Hello")
             )
        initialSnapshot <- ZWorkflowStub.query(
                             workflowStub.messages
                           )
        _ <- ZWorkflowStub.signal(
               workflowStub.echo("World!")
             )
        secondSnapshot <- ZWorkflowStub.query(
                            workflowStub.messages
                          )
        _ <- ZWorkflowStub.signal(
               workflowStub.echo("Again...")
             )
        thirdSnapshot <- ZWorkflowStub.query(
                           workflowStub.messages
                         )
        _ <- ZWorkflowStub.signal(
               workflowStub.stop()
             )
        result <- workflowStub.result[Int]
      } yield {
        assertTrue(
          initialSnapshot == List("Hello"),
          secondSnapshot == List("Hello", "World!"),
          thirdSnapshot == List("Hello", "World!", "Again..."),
          result == 3
        )
      }

    }.provideTestWorkflowEnv @@ TestAspect.flaky,
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
                       ZActivityType[TransferActivity] ->
                         ZActivityOptions
                           .withStartToCloseTimeout(5.seconds)
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
    }.provideTestWorkflowEnv,
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
    }.provideTestWorkflowEnv
  )
}
