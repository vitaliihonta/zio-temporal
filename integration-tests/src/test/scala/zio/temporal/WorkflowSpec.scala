package zio.temporal

import zio.*
import zio.temporal.activity.ZActivityOptions
import zio.temporal.fixture.*
import zio.temporal.internal.TemporalWorkflowFacade
import zio.temporal.signal.*
import zio.temporal.testkit.*
import zio.temporal.worker.*
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
      val taskQueue = "sample-queue"
      val sampleIn  = "Fooo"
      val sampleOut = sampleIn

      for {
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SampleWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        sampleWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                            _.newWorkflowStub[SampleWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(UUID.randomUUID().toString)
                              .withWorkflowRunTimeout(10.second)
                              .build
                          )
        result <- ZWorkflowStub.execute(sampleWorkflow.echo(sampleIn))
      } yield assertTrue(result == sampleOut)

    }.provideEnv,
    test("runs child workflows") {
      val taskQueue = "child-queue"

      for {
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[GreetingWorkflowImpl].fromClass @@
               ZWorker.addWorkflow[GreetingChildImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        greetingWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                              _.newWorkflowStub[GreetingWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(UUID.randomUUID().toString)
                                .withWorkflowRunTimeout(10.second)
                                .build
                            )
        result <- ZWorkflowStub.execute(greetingWorkflow.getGreeting("Vitalii"))
      } yield assertTrue(result == "Hello Vitalii!")

    }.provideEnv,
    test("run workflow with signals") {
      val taskQueue  = "signal-queue"
      val workflowId = UUID.randomUUID().toString + taskQueue

      for {
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SignalWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        signalWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                            _.newWorkflowStub[SignalWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(workflowId)
                              .withWorkflowRunTimeout(30.seconds)
                              .withWorkflowTaskTimeout(30.seconds)
                              .withWorkflowExecutionTimeout(30.seconds)
                              .build
                          )
        _ <- ZIO.log("Before start")
        _ <- ZWorkflowStub.start(
               signalWorkflow.echoServer("ECHO")
             )
        _ <- ZIO.log("Started")
        workflowStub <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                          _.newWorkflowStubProxy[SignalWorkflow](workflowId)
                        )
        _ <- ZIO.log("New stub created!")
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

    }.provideEnv,
    test("run workflow with ZIO") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue  = "zio-queue"
        val workflowId = UUID.randomUUID().toString + taskQueue

        for {
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[ZioWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new ZioActivityImpl()(activityOptions))
          _ <- ZTestWorkflowEnvironment.setup()
          zioWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                           _.newWorkflowStub[ZioWorkflow]
                             .withTaskQueue(taskQueue)
                             .withWorkflowId(workflowId)
                             .withWorkflowRunTimeout(30.seconds)
                             .withWorkflowTaskTimeout(30.seconds)
                             .withWorkflowExecutionTimeout(30.seconds)
                             .build
                         )
          _ <- ZWorkflowStub.start(
                 zioWorkflow.echo("HELLO THERE")
               )
          workflowStub <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                            _.newWorkflowStubProxy[ZioWorkflow](workflowId)
                          )
          _ <- ZWorkflowStub.signal(
                 workflowStub.complete()
               )
          result <- workflowStub.result[String]
        } yield assertTrue(result == "Echoed HELLO THERE")
      }

    }.provideEnv,
    test("run workflow with signal and start") {
      val taskQueue  = "signal-with-start-queue"
      val workflowId = UUID.randomUUID().toString

      for {
        _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
               ZWorker.addWorkflow[SignalWithStartWorkflowImpl].fromClass
        _ <- ZTestWorkflowEnvironment.setup()
        signalWithStartWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                                     _.newWorkflowStub[SignalWithStartWorkflow]
                                       .withTaskQueue(taskQueue)
                                       .withWorkflowId(workflowId)
                                       .withWorkflowRunTimeout(10.seconds)
                                       .build
                                   )
        _ <- ZTestWorkflowEnvironment.workflowClientWithZIO(
               _ signalWith (
                 signalWithStartWorkflow.echo("Hello")
               ) start (
                 signalWithStartWorkflow.echoServer()
               )
             )
        workflowStub <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                          _.newWorkflowStubProxy[SignalWithStartWorkflow](workflowId)
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

    }.provideEnv @@ TestAspect.flaky,
    test("run workflow with successful sagas") {
      ZTestWorkflowEnvironment.activityOptionsWithZIO[Any] { implicit activityOptions =>
        val taskQueue      = "saga-queue"
        val successFunc    = (_: String, _: BigDecimal) => ZIO.succeed(Done())
        val expectedResult = BigDecimal(5.0)
        val workflowId     = UUID.randomUUID().toString

        for {
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[SagaWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new TransferActivityImpl(successFunc, successFunc))
          _ <- ZTestWorkflowEnvironment.setup()
          signalWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                              _.newWorkflowStub[SagaWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(workflowId)
                                .withWorkflowRunTimeout(10.seconds)
                                .build
                            )
          result <- ZWorkflowStub.execute(
                      signalWorkflow.transfer(TransferCommand("from", "to", expectedResult))
                    )
        } yield {
          assertTrue(result == expectedResult)
        }
      }

    }.provideEnv,
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

        val amount     = BigDecimal(5.0)
        val workflowId = UUID.randomUUID().toString

        for {
          _ <- ZTestWorkflowEnvironment.newWorker(taskQueue) @@
                 ZWorker.addWorkflow[SagaWorkflowImpl].fromClass @@
                 ZWorker.addActivityImplementation(new TransferActivityImpl(depositFunc, withdrawFunc))
          _ <- ZTestWorkflowEnvironment.setup()
          sagaWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                            _.newWorkflowStub[SagaWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(workflowId)
                              .withWorkflowRunTimeout(10.seconds)
                              .build
                          )
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
      val taskQueue = "promise-queue"

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

      val tests = for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        promiseWorkflow <- ZTestWorkflowEnvironment.workflowClientWithZIO(
                             _.newWorkflowStub[PromiseWorkflow]
                               .withTaskQueue(taskQueue)
                               .withWorkflowId(workflowId.toString)
                               .withWorkflowRunTimeout(10.seconds)
                               .build
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
                        .collectAll((1 to 20).map(_ => tests))
                        .map { res =>
                          val actualResult = order.get.toList

                          println(actualResult.toString)

                          TestResult.all(res: _*) &&
                          assertTrue(actualResult.size == 40) &&
                          assertTrue(actualResult != List.fill(20)(List("foo" -> x, "bar" -> y)))
                        }
      } yield assertions

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
