package zio.temporal

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.fixture._
import zio.temporal.testkit._
import zio.temporal.worker._
import zio.temporal.workflow._
import zio.test._

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
      } yield {
        assertTrue(result == "Done")
      }

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

    }.provideTestWorkflowEnv @@ TestAspect.flaky
  )
}
