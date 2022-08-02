package zio.temporal

import zio._
import zio.temporal.fixture.Done
import zio.temporal.fixture.SagaWorkflow
import zio.temporal.fixture.SagaWorkflowImpl
import zio.temporal.fixture._
import zio.temporal.signal._
import zio.temporal.testkit.ZTestEnvironmentOptions
import zio.temporal.testkit.ZTestWorkflowEnvironment
import zio.temporal.worker.ZWorkerOptions
import zio.temporal.workflow._
import zio.test._
import zio.test.TestAspect._

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import scala.collection.mutable.ListBuffer

object WorkflowSpec extends ZIOSpecDefault {

  override def spec = suite("ZWorkflow")(
//    test("runs simple workflow") {
//      ZIO.serviceWithZIO[ZTestWorkflowEnvironment] { testEnv =>
//        val taskQueue = "sample-queue"
//        val sampleIn  = "Fooo"
//        val sampleOut = sampleIn
//        val client    = testEnv.workflowClient
//
//        testEnv
//          .newWorker(taskQueue, options = ZWorkerOptions.default)
//          .addWorkflow[SampleWorkflowImpl]
//          .fromClass
//
//        withWorkflow {
//          testEnv.use() {
//            for {
//              sampleWorkflow <- client
//                                  .newWorkflowStub[SampleWorkflow]
//                                  .withTaskQueue(taskQueue)
//                                  .withWorkflowId(UUID.randomUUID().toString)
//                                  .withWorkflowRunTimeout(1.second)
//                                  .build
//              result <- ZWorkflowStub.execute(sampleWorkflow.echo(sampleIn))
//            } yield assertTrue(result == sampleOut)
//          }
//        }
//      }
//    }.provideEnv,
    test("run workflow with signals") {
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment] { testEnv =>
        val taskQueue = "signal-queue"
        val client    = testEnv.workflowClient

        testEnv
          .newWorker(taskQueue, options = ZWorkerOptions.default)
          .addWorkflow[SignalWorkflowImpl]
          .fromClass

        val workflowId = UUID.randomUUID().toString

        withWorkflow {
          testEnv.use() {
            for {
              signalWorkflow <- client
                                  .newWorkflowStub[SignalWorkflow]
                                  .withTaskQueue(taskQueue)
                                  .withWorkflowId(workflowId)
                                  .withWorkflowRunTimeout(10.seconds)
                                  .build
              _            <- ZWorkflowStub.start(signalWorkflow.echoServer("ECHO"))
              workflowStub <- client.newWorkflowStubProxy[SignalWorkflow](workflowId)
//              progress     <- ZWorkflowStub.query(workflowStub.getProgress)
//              _ <- ZWorkflowStub.signal(
//                     workflowStub.echo("Hello!")
//                   )
//              progress2 <- ZWorkflowStub
//                             .query(workflowStub.getProgress)
//                             .repeatWhile(_.isEmpty)
              result <- workflowStub.result[String]
            } yield /*assertTrue(progress.isEmpty) &&
              assertTrue(progress2.contains("Hello!")) &&
              */assertTrue(result == "ECHO Hello!")
          }
        }
      }
    }.provideEnv,
//    test("run workflow with signal and start") {
//      ZIO.serviceWithZIO[ZTestWorkflowEnvironment] { testEnv =>
//        val taskQueue = "signal-with-start-queue"
//        val client    = testEnv.workflowClient
//
//        testEnv
//          .newWorker(taskQueue, options = ZWorkerOptions.default)
//          .addWorkflow[SignalWithStartWorkflowImpl]
//          .fromClass
//
//        val workflowId = UUID.randomUUID().toString
//
//        withWorkflow {
//          testEnv.use() {
//            for {
//              signalWithStartWorkflow <- client
//                                           .newWorkflowStub[SignalWithStartWorkflow]
//                                           .withTaskQueue(taskQueue)
//                                           .withWorkflowId(workflowId)
//                                           .withWorkflowRunTimeout(10.seconds)
//                                           .build
//              _ <- client
//                     .signalWith(
//                       signalWithStartWorkflow.echo("Hello")
//                     )
//                     .start(signalWithStartWorkflow.echoServer())
//              workflowStub <- client.newWorkflowStubProxy[SignalWithStartWorkflow](workflowId)
//              initialSnapshot <- ZWorkflowStub.query(
//                                   workflowStub.messages
//                                 )
//              _ <- ZWorkflowStub.signal(
//                     workflowStub.echo("World!")
//                   )
//              secondSnapshot <- ZWorkflowStub.query(
//                                  workflowStub.messages
//                                )
//              _ <- ZWorkflowStub.signal(
//                     workflowStub.echo("Again...")
//                   )
//              thirdSnapshot <- ZWorkflowStub.query(
//                                 workflowStub.messages
//                               )
//              _ <- ZWorkflowStub.signal(
//                     workflowStub.stop()
//                   )
//              result <- workflowStub.result[Int]
//            } yield assertTrue(initialSnapshot == List("Hello")) &&
//              assertTrue(secondSnapshot == List("Hello", "World!")) &&
//              assertTrue(thirdSnapshot == List("Hello", "World!", "Again...")) &&
//              assertTrue(result == 3)
//          }
//        }
//      }
//    }.provideEnv,
//    test("run workflow with successful sagas") {
//      ZIO.serviceWithZIO[ZTestWorkflowEnvironment] { testEnv =>
//        val taskQueue = "saga-queue"
//        val client    = testEnv.workflowClient
//
//        val successFunc = (_: String, _: BigDecimal) => Right(Done())
//
//        val expectedResult = BigDecimal(5.0)
//
//        testEnv
//          .newWorker(taskQueue, options = ZWorkerOptions.default)
//          .addWorkflow[SagaWorkflowImpl]
//          .fromClass
//          .addActivityImplementation(new TransferActivityImpl(successFunc, successFunc))
//
//        val workflowId = UUID.randomUUID().toString
//
//        withWorkflow {
//          testEnv.use() {
//            for {
//              signalWorkflow <- client
//                                  .newWorkflowStub[SagaWorkflow]
//                                  .withTaskQueue(taskQueue)
//                                  .withWorkflowId(workflowId)
//                                  .withWorkflowRunTimeout(10.seconds)
//                                  .build
//              result <- ZWorkflowStub.execute(signalWorkflow.transfer(TransferCommand("from", "to", expectedResult)))
//            } yield assertTrue(result == expectedResult)
//          }
//        }
//      }
//    }.provideEnv,
//    test("run workflow with saga compensations") {
//      ZIO.serviceWithZIO[ZTestWorkflowEnvironment] { testEnv =>
//        val taskQueue = "saga-queue"
//        val client    = testEnv.workflowClient
//        val From      = "from"
//        val To        = "to"
//
//        val compensated  = new AtomicBoolean(false)
//        val error        = Error("fraud")
//        val withdrawFunc = (_: String, _: BigDecimal) => Right(Done())
//        val depositFunc: (String, BigDecimal) => Either[fixture.Error, Done] = {
//          case (From, _) =>
//            compensated.set(true)
//            Right(Done())
//          case _ =>
//            Left(error)
//        }
//
//        val amount = BigDecimal(5.0)
//
//        testEnv
//          .newWorker(taskQueue, options = ZWorkerOptions.default)
//          .addWorkflow[SagaWorkflowImpl]
//          .fromClass
//          .addActivityImplementation(new TransferActivityImpl(depositFunc, withdrawFunc))
//
//        val workflowId = UUID.randomUUID().toString
//
//        withWorkflow {
//          testEnv.use() {
//            for {
//              sagaWorkflow <- client
//                                .newWorkflowStub[SagaWorkflow]
//                                .withTaskQueue(taskQueue)
//                                .withWorkflowId(workflowId)
//                                .withWorkflowRunTimeout(10.seconds)
//                                .build
//              result <- ZWorkflowStub
//                          .execute(sagaWorkflow.transfer(TransferCommand(From, To, amount)))
//                          .either
//            } yield {
//              val actualError = result.left.getOrElse(???).getError.get
//              assertTrue(actualError == error) &&
//              assertTrue(compensated.get())
//            }
//          }
//        }
//      }
//    }.provideEnv,
//    test("run workflow with promise") {
//      ZIO.serviceWithZIO[ZTestWorkflowEnvironment] { testEnv =>
//        val taskQueue = "promise-queue"
//        val client    = testEnv.workflowClient
//
//        val order = new AtomicReference(ListBuffer.empty[(String, Int)])
//        val fooFunc = (x: Int) => {
//          println(s"foo($x)")
//          order.get += ("foo" -> x)
//          x
//        }
//        val barFunc = (x: Int) => {
//          println(s"bar($x)")
//          order.get += ("bar" -> x)
//          x
//        }
//
//        testEnv
//          .newWorker(taskQueue, options = ZWorkerOptions.default)
//          .addWorkflow[PromiseWorkflowImpl]
//          .fromClass
//          .addActivityImplementation(new PromiseActivityImpl(fooFunc, barFunc))
//
//        val x = 2
//        val y = 3
//
//        withWorkflow {
//          testEnv.use() {
//            val tests = for {
//              workflowId <- ZIO.randomWith(_.nextUUID)
//              promiseWorkflow <- client
//                                   .newWorkflowStub[PromiseWorkflow]
//                                   .withTaskQueue(taskQueue)
//                                   .withWorkflowId(workflowId.toString)
//                                   .withWorkflowRunTimeout(10.seconds)
//                                   .build
//
//              result <- ZWorkflowStub.execute(promiseWorkflow.fooBar(x, y))
//            } yield assertTrue(result == x + y)
//
//            tests.repeatN(19).map { res =>
//              val actualResult = order.get.toList
//
//              println(actualResult.toString)
//
//              res &&
//              assertTrue(actualResult.size == 40) &&
//              assertTrue(actualResult != List.fill(20)(List("foo" -> x, "bar" -> y)))
//            }
//          }
//        }
//      }
//    }.provideEnv
  )

  private def withWorkflow[R, E, A](f: ZIO[R, TemporalError[E], A]): RIO[R, A] =
    f.mapError(e => new RuntimeException(s"IO failed with $e"))

  private implicit class ProvidedTestkit[E, A](thunk: Spec[ZTestWorkflowEnvironment, E]) {
    def provideEnv: Spec[Any, E] =
      thunk.provide(
        ZLayer.succeed(ZTestEnvironmentOptions.default),
        ZTestWorkflowEnvironment.make
      )
  }
}
