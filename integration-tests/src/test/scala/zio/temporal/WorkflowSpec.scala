package zio.temporal

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.test._
import zio.test.TestAspect._
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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
        sampleWorkflow <- ZTestWorkflowEnvironment
                            .newWorkflowStub[SampleWorkflow]
                            .withTaskQueue(taskQueue)
                            .withWorkflowId(workflowId.toString)
                            .withWorkflowRunTimeout(10.second)
                            .build
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
        sampleWorkflow <- ZTestWorkflowEnvironment
                            .newWorkflowStub[SampleNamedWorkflow]
                            .withTaskQueue(taskQueue)
                            .withWorkflowId(workflowId.toString)
                            .withWorkflowRunTimeout(10.second)
                            .build
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
        greetingWorkflow <- ZTestWorkflowEnvironment
                              .newWorkflowStub[GreetingWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(workflowId.toString)
                              .withWorkflowRunTimeout(10.second)
                              .build
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
        greetingWorkflow <- ZTestWorkflowEnvironment
                              .newWorkflowStub[GreetingNamedWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(workflowId.toString)
                              .withWorkflowRunTimeout(10.second)
                              .build
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
        greetingWorkflow <- ZTestWorkflowEnvironment
                              .newUntypedWorkflowStub("GreetingUntypedWorkflow")
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(workflowId.toString)
                              .withWorkflowRunTimeout(10.second)
                              .build
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
        fooWorkflow <- ZTestWorkflowEnvironment
                         .newWorkflowStub[WorkflowFoo]
                         .withTaskQueue(taskQueue)
                         .withWorkflowId(fooWorkflowId)
                         .withWorkflowRunTimeout(10.second)
                         .build
        barWorkflowId = fooWorkflowId + "-bar"
        barWorkflow <- ZTestWorkflowEnvironment
                         .newWorkflowStub[WorkflowBar]
                         .withTaskQueue(taskQueue)
                         .withWorkflowId(barWorkflowId)
                         .withWorkflowRunTimeout(10.second)
                         .build
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
        fooWorkflow <- ZTestWorkflowEnvironment
                         .newUntypedWorkflowStub("WorkflowFooUntyped")
                         .withTaskQueue(taskQueue)
                         .withWorkflowId(fooWorkflowId)
                         .withWorkflowRunTimeout(10.second)
                         .build
        barWorkflowId = fooWorkflowId + "-bar"
        barWorkflow <- ZTestWorkflowEnvironment
                         .newUntypedWorkflowStub("WorkflowBarUntyped")
                         .withTaskQueue(taskQueue)
                         .withWorkflowId(barWorkflowId)
                         .withWorkflowRunTimeout(10.second)
                         .build
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
        signalWorkflow <- ZTestWorkflowEnvironment
                            .newWorkflowStub[SignalWorkflow]
                            .withTaskQueue(taskQueue)
                            .withWorkflowId(workflowId)
                            .withWorkflowRunTimeout(30.seconds)
                            .withWorkflowTaskTimeout(30.seconds)
                            .withWorkflowExecutionTimeout(30.seconds)
                            .build
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
        assertTrue(progress.isEmpty) &&
        assertTrue(progressDefault.contains("default")) &&
        assertTrue(progress2.contains("Hello!")) &&
        assertTrue(result == "ECHO Hello!")
      }

    }.provideTestWorkflowEnv,
    test("run workflow with untyped signals") {
      val taskQueue = "signal-untyped-queue"

      for {
        workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SignalWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        signalWorkflow <- ZTestWorkflowEnvironment
                            .newUntypedWorkflowStub("SignalWorkflow")
                            .withTaskQueue(taskQueue)
                            .withWorkflowId(workflowId)
                            .withWorkflowRunTimeout(30.seconds)
                            .withWorkflowTaskTimeout(30.seconds)
                            .withWorkflowExecutionTimeout(30.seconds)
                            .build
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
        assertTrue(progress.isEmpty) &&
        assertTrue(progressDefault.contains("default")) &&
        assertTrue(progress2.contains("Hello!")) &&
        assertTrue(result == "ECHO Hello!")
      }

    }.provideTestWorkflowEnv,
    test("run workflow with ZIO") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue = "zio-queue"

        for {
          workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ZioWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new ZioActivityImpl()(activityOptions))
          _ <- ZTestWorkflowEnvironment.setup()
          zioWorkflow <- ZTestWorkflowEnvironment
                           .newWorkflowStub[ZioWorkflow]
                           .withTaskQueue(taskQueue)
                           .withWorkflowId(workflowId)
                           .withWorkflowRunTimeout(30.seconds)
                           .withWorkflowTaskTimeout(30.seconds)
                           .withWorkflowExecutionTimeout(30.seconds)
                           .build
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
    test("run workflow with continue as new") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue = "continue-as-new-queue"

        for {
          workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)

          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ContinueAsNewWorkflowImpl].fromClass

          _ <- ZTestWorkflowEnvironment.setup()
          workflow <- ZTestWorkflowEnvironment
                        .newWorkflowStub[ContinueAsNewWorkflow]
                        .withTaskQueue(taskQueue)
                        .withWorkflowId(workflowId)
                        .withWorkflowRunTimeout(30.seconds)
                        .withWorkflowTaskTimeout(30.seconds)
                        .withWorkflowExecutionTimeout(30.seconds)
                        .build
          result <- ZWorkflowStub.execute(
                      workflow.doSomething(0)
                    )
        } yield assertTrue(result == "Done")
      }

    }.provideTestWorkflowEnv,
    test("run custom-named workflow with continue as new") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue = "continue-as-new-named-queue"

        for {
          workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)

          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ContinueAsNewNamedWorkflowImpl].fromClass

          _ <- ZTestWorkflowEnvironment.setup()
          workflow <- ZTestWorkflowEnvironment
                        .newWorkflowStub[ContinueAsNewNamedWorkflow]
                        .withTaskQueue(taskQueue)
                        .withWorkflowId(workflowId)
                        .withWorkflowRunTimeout(30.seconds)
                        .withWorkflowTaskTimeout(30.seconds)
                        .withWorkflowExecutionTimeout(30.seconds)
                        .build
          result <- ZWorkflowStub.execute(
                      workflow.doSomething(0)
                    )
        } yield assertTrue(result == "Done")
      }

    }.provideTestWorkflowEnv,
    test("run workflow with ZIO untyped activity") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue = "zio-untyped-queue"

        for {
          workflowId <- ZIO.randomWith(_.nextUUID).map(_.toString + taskQueue)

          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ZioWorkflowUntypedImpl].fromClass @@
                 ZWorker.addActivityImplementation(new ZioUntypedActivityImpl())
          _ <- ZTestWorkflowEnvironment.setup()
          zioWorkflow <- ZTestWorkflowEnvironment
                           .newUntypedWorkflowStub("ZioWorkflowUntyped")
                           .withTaskQueue(taskQueue)
                           .withWorkflowId(workflowId)
                           .withWorkflowRunTimeout(30.seconds)
                           .withWorkflowTaskTimeout(30.seconds)
                           .withWorkflowExecutionTimeout(30.seconds)
                           .build
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
        workflowStub <- ZTestWorkflowEnvironment
                          .newWorkflowStub[SignalWithStartWorkflow]
                          .withTaskQueue(taskQueue)
                          .withWorkflowId(workflowId.toString)
                          .withWorkflowRunTimeout(10.seconds)
                          .build
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
        assertTrue(initialSnapshot == List("Hello")) &&
        assertTrue(secondSnapshot == List("Hello", "World!")) &&
        assertTrue(thirdSnapshot == List("Hello", "World!", "Again...")) &&
        assertTrue(result == 3)
      }

    }.provideTestWorkflowEnv @@ TestAspect.flaky,
    test("run workflow with successful sagas") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue      = "saga-queue"
        val successFunc    = (_: String, _: BigDecimal) => ZIO.succeed(Done())
        val expectedResult = BigDecimal(5.0)

        for {
          workflowId <- ZIO.randomWith(_.nextUUID)

          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[SagaWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new TransferActivityImpl(successFunc, successFunc))
          _ <- ZTestWorkflowEnvironment.setup()
          sagaWorkflow <- ZTestWorkflowEnvironment
                            .newWorkflowStub[SagaWorkflow]
                            .withTaskQueue(taskQueue)
                            .withWorkflowId(workflowId.toString)
                            .withWorkflowRunTimeout(10.seconds)
                            .build
          result <- ZWorkflowStub.executeWithTimeout(5.seconds)(
                      sagaWorkflow.transfer(TransferCommand("from", "to", expectedResult))
                    )
        } yield {
          assertTrue(result == expectedResult)
        }
      }

    }.provideTestWorkflowEnv,
    test("run workflow with saga compensations") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue = "saga-queue"

        val From = "from"
        val To   = "to"

        val compensated  = new AtomicBoolean(false)
        val error        = TransferError("fraud")
        val withdrawFunc = (_: String, _: BigDecimal) => ZIO.succeed(Done())
        val depositFunc: (String, BigDecimal) => IO[TransferError, Done] = {
          case (From, _) =>
            compensated.set(true)
            ZIO.succeed(Done())
          case _ =>
            ZIO.fail(error)
        }

        val amount = BigDecimal(5.0)

        for {
          workflowId <- ZIO.randomWith(_.nextUUID)

          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[SagaWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new TransferActivityImpl(depositFunc, withdrawFunc))
          _ <- ZTestWorkflowEnvironment.setup()
          sagaWorkflow <- ZTestWorkflowEnvironment
                            .newWorkflowStub[SagaWorkflow]
                            .withTaskQueue(taskQueue)
                            .withWorkflowId(workflowId.toString)
                            .withWorkflowRunTimeout(10.seconds)
                            .build
          result <- ZWorkflowStub
                      .execute(sagaWorkflow.transfer(TransferCommand(From, To, amount)))
                      .either
        } yield {
          val actualError = result.left.getOrElse(???)
          println("-" * 20)
          println(s"Actual error: $actualError")
          println("-" * 20)

          assertTrue(compensated.get())
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
        promiseWorkflow <- ZTestWorkflowEnvironment
                             .newWorkflowStub[PromiseWorkflow]
                             .withTaskQueue(taskQueue)
                             .withWorkflowId(s"zasync-spec-$n")
                             .withWorkflowRunTimeout(10.seconds)
                             .build
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

                          results.reduce(_ && _) &&
                          assertTrue(actualResult.size == 40) &&
                          assertTrue(actualResult != List.fill(20)(List("foo" -> x, "bar" -> y)))
                        }
      } yield assertions
    }.provideTestWorkflowEnv @@ TestAspect.flaky(3),
    test("run parameterized workflow") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
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

          sodaWf <- ZTestWorkflowEnvironment
                      .newWorkflowStub[SodaWorkflow]
                      .withTaskQueue(taskQueue)
                      .withWorkflowId(s"soda/$uuid")
                      .build

          juiceWf <- ZTestWorkflowEnvironment
                       .newWorkflowStub[JuiceWorkflow]
                       .withTaskQueue(taskQueue)
                       .withWorkflowId(s"juice/$uuid")
                       .build

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
        paymentWorkflow <- ZTestWorkflowEnvironment
                             .newWorkflowStub[PaymentWorkflow]
                             .withTaskQueue(taskQueue)
                             .withWorkflowId(workflowId.toString)
                             // Set workflow timeout
                             .withWorkflowRunTimeout(20.minutes)
                             .build

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

        retryWorkflow <- ZTestWorkflowEnvironment
                           .newWorkflowStub[RetryWorkflow]
                           .withTaskQueue(taskQueue)
                           .withWorkflowId(workflowId.toString)
                           .withWorkflowRunTimeout(10.second)
                           .build

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

        memoWorkflow <- ZTestWorkflowEnvironment
                          .newWorkflowStub[MemoWorkflow]
                          .withTaskQueue(taskQueue)
                          .withWorkflowId(workflowId.toString)
                          .withMemo("zio" -> "temporal")
                          .withWorkflowRunTimeout(10.second)
                          .build

        result <- ZWorkflowStub.execute(memoWorkflow.withMemo("zio"))
      } yield assert(result)(Assertion.isSome(Assertion.equalTo("temporal")))
    }.provideTestWorkflowEnv
  )
}
