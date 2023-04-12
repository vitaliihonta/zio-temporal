package zio.temporal

import zio.*
import zio.temporal.activity.ZActivityOptions
import zio.temporal.fixture.Done
import zio.temporal.fixture.SagaWorkflow
import zio.temporal.fixture.SagaWorkflowImpl
import zio.temporal.fixture.*
import zio.temporal.internal.TemporalWorkflowFacade
import zio.temporal.signal.*
import zio.temporal.testkit.ZTestEnvironmentOptions
import zio.temporal.testkit.ZTestWorkflowEnvironment
import zio.temporal.worker.ZWorkerOptions
import zio.temporal.workflow.*
import zio.test.*
import zio.test.TestAspect.*

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.ListBuffer

object WorkflowSpec extends ZIOSpecDefault {

  override def spec = suite("ZWorkflow")(
    test("runs simple workflow") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        val taskQueue = "sample-queue"
        val sampleIn  = "Fooo"
        val sampleOut = sampleIn
        val client    = testEnv.workflowClient

        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[SampleWorkflowImpl]
          .fromClass

        for {
          _ <- testEnv.setup()
          sampleWorkflow <- client
                              .newWorkflowStub[SampleWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(UUID.randomUUID().toString)
                              .withWorkflowRunTimeout(10.second)
                              .build
          result <- ZWorkflowStub.execute(sampleWorkflow.echo(sampleIn))
        } yield assertTrue(result == sampleOut)
      }
    }.provideEnv,
    test("runs child workflows") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        val taskQueue = "child-queue"
        val client    = testEnv.workflowClient

        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[GreetingWorkflowImpl]
          .fromClass
          .addWorkflow[GreetingChildImpl]
          .fromClass
        for {
          _ <- testEnv.setup()
          greetingWorkflow <- client
                                .newWorkflowStub[GreetingWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(UUID.randomUUID().toString)
                                .withWorkflowRunTimeout(10.second)
                                .build
          result <- ZWorkflowStub.execute(greetingWorkflow.getGreeting("Vitalii"))
        } yield assertTrue(result == "Hello Vitalii!")
      }
    }.provideEnv,
    test("run workflow with signals") {
      ZIO
        .serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
          val taskQueue = "signal-queue"
          val client    = testEnv.workflowClient

          testEnv
            .newWorker(taskQueue, options = ZWorkerOptions.default)
            .addWorkflow[SignalWorkflowImpl]
            .fromClass

          val workflowId = UUID.randomUUID().toString + taskQueue

          for {
            _ <- testEnv.setup()
            signalWorkflow <- client
                                .newWorkflowStub[SignalWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(workflowId)
                                .withWorkflowRunTimeout(30.seconds)
                                .withWorkflowTaskTimeout(30.seconds)
                                .withWorkflowExecutionTimeout(30.seconds)
                                .build
            _ <- ZIO.log("Before start")
            _ <- ZWorkflowStub
                   .start(signalWorkflow.echoServer("ECHO"))
            _               <- ZIO.log("Started")
            workflowStub    <- client.newWorkflowStubProxy[SignalWorkflow](workflowId)
            _               <- ZIO.log("New stub created!")
            progress        <- ZWorkflowStub.query(workflowStub.getProgress(default = None))
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
          } yield assertTrue(progress.isEmpty) &&
            assertTrue(progressDefault.contains("default")) &&
            assertTrue(progress2.contains("Hello!")) &&
            assertTrue(result == "ECHO Hello!")
        }
    }.provideEnv,
    test("run workflow with ZIO") {
      ZIO
        .serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
          val taskQueue = "zio-queue"
          val client    = testEnv.workflowClient
          import testEnv.activityOptions

          testEnv
            .newWorker(taskQueue, options = ZWorkerOptions.default)
            .addWorkflow[ZioWorkflowImpl]
            .fromClass
            .addActivityImplementation(new ZioActivityImpl())

          val workflowId = UUID.randomUUID().toString + taskQueue

          for {
            _ <- testEnv.setup()
            zioWorkflow <- client
                             .newWorkflowStub[ZioWorkflow]
                             .withTaskQueue(taskQueue)
                             .withWorkflowId(workflowId)
                             .withWorkflowRunTimeout(30.seconds)
                             .withWorkflowTaskTimeout(30.seconds)
                             .withWorkflowExecutionTimeout(30.seconds)
                             .build
            _ <- ZWorkflowStub
                   .start(zioWorkflow.echo("HELLO THERE"))
            workflowStub <- client.newWorkflowStubProxy[ZioWorkflow](workflowId)
            _ <- ZWorkflowStub.signal(
                   workflowStub.complete()
                 )
            result <- workflowStub.result[String]
          } yield assertTrue(result == "Echoed HELLO THERE")
        }
    }.provideEnv,
    test("run workflow with signal and start") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        val taskQueue = "signal-with-start-queue"
        val client    = testEnv.workflowClient

        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[SignalWithStartWorkflowImpl]
          .fromClass

        val workflowId = UUID.randomUUID().toString

        for {
          _ <- testEnv.setup()
          signalWithStartWorkflow <- client
                                       .newWorkflowStub[SignalWithStartWorkflow]
                                       .withTaskQueue(taskQueue)
                                       .withWorkflowId(workflowId)
                                       .withWorkflowRunTimeout(10.seconds)
                                       .build
          _ <- client
                 .signalWith(
                   signalWithStartWorkflow.echo("Hello")
                 )
                 .start(signalWithStartWorkflow.echoServer())
          workflowStub <- client.newWorkflowStubProxy[SignalWithStartWorkflow](workflowId)
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
        } yield assertTrue(initialSnapshot == List("Hello")) &&
          assertTrue(secondSnapshot == List("Hello", "World!")) &&
          assertTrue(thirdSnapshot == List("Hello", "World!", "Again...")) &&
          assertTrue(result == 3)
      }
    }.provideEnv @@ TestAspect.flaky,
    test("run workflow with successful sagas") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        val taskQueue = "saga-queue"
        val client    = testEnv.workflowClient
        import testEnv.activityOptions

        val successFunc = (_: String, _: BigDecimal) => ZIO.succeed(Done())

        val expectedResult = BigDecimal(5.0)

        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[SagaWorkflowImpl]
          .fromClass
          .addActivityImplementation(new TransferActivityImpl(successFunc, successFunc))

        val workflowId = UUID.randomUUID().toString

        for {
          _ <- testEnv.setup()
          signalWorkflow <- client
                              .newWorkflowStub[SagaWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(workflowId)
                              .withWorkflowRunTimeout(10.seconds)
                              .build
          result <- ZWorkflowStub.execute(signalWorkflow.transfer(TransferCommand("from", "to", expectedResult)))
        } yield assertTrue(result == expectedResult)
      }
    }.provideEnv,
    test("run workflow with saga compensations") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        val taskQueue = "saga-queue"
        val client    = testEnv.workflowClient
        import testEnv.activityOptions

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

        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[SagaWorkflowImpl]
          .fromClass
          .addActivityImplementation(new TransferActivityImpl(depositFunc, withdrawFunc))

        val workflowId = UUID.randomUUID().toString

        for {
          _ <- testEnv.setup()
          sagaWorkflow <- client
                            .newWorkflowStub[SagaWorkflow]
                            .withTaskQueue(taskQueue)
                            .withWorkflowId(workflowId)
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
    }.provideEnv,
    test("run workflow with promise") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        val taskQueue = "promise-queue"
        val client    = testEnv.workflowClient

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

        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[PromiseWorkflowImpl]
          .fromClass
          .addActivityImplementation(new PromiseActivityImpl(fooFunc, barFunc))

        val x = 2
        val y = 3

        val tests = for {
          workflowId <- ZIO.randomWith(_.nextUUID)
          promiseWorkflow <- client
                               .newWorkflowStub[PromiseWorkflow]
                               .withTaskQueue(taskQueue)
                               .withWorkflowId(workflowId.toString)
                               .withWorkflowRunTimeout(10.seconds)
                               .build

          result <- ZWorkflowStub.execute(promiseWorkflow.fooBar(x, y))
        } yield assertTrue(result == x + y)

        testEnv.setup() *> tests.repeatN(19).map { res =>
          val actualResult = order.get.toList

          println(actualResult.toString)

          res &&
          assertTrue(actualResult.size == 40) &&
          assertTrue(actualResult != List.fill(20)(List("foo" -> x, "bar" -> y)))
        }
      }
    }.provideEnv @@ TestAspect.flaky
  )

  private implicit class ProvidedTestkit[E, A](thunk: Spec[ZTestWorkflowEnvironment[Any] with Scope, E]) {
    def provideEnv: Spec[Scope, E] =
      thunk.provideSome[Scope](
        ZLayer.succeed(ZTestEnvironmentOptions.default),
        ZTestWorkflowEnvironment.make[Any]
      )
  }
}
