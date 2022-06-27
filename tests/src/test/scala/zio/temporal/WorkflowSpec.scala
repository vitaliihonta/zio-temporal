package zio.temporal

import zio._
import zio.temporal.fixture.Done
import zio.temporal.fixture.SagaWorkflow
import zio.temporal.fixture.SagaWorkflowImpl
import zio.temporal.fixture._
import zio.temporal.signal._
import zio.temporal.testkit.ZTestWorkflowEnvironment
import zio.temporal.worker.ZWorkerOptions
import zio.temporal.workflow._

import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class WorkflowSpec extends BaseSpec {
  "ZWorkflow" should {
    "run simple workflow" in { (testEnv: ZTestWorkflowEnvironment) =>
      val taskQueue = "sample-queue"
      val sampleIn  = "Fooo"
      val sampleOut = sampleIn
      val client    = testEnv.workflowClient

      testEnv
        .newWorker(taskQueue, options = ZWorkerOptions.default)
        .addWorkflow[SampleWorkflowImpl]
        .fromClass

      withWorkflow {
        testEnv.use() {
          for {
            sampleWorkflow <- client
                                .newWorkflowStub[SampleWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(UUID.randomUUID().toString)
                                .withWorkflowRunTimeout(1.second)
                                .build
            result <- (sampleWorkflow.echo _).execute(sampleIn)
          } yield result shouldEqual sampleOut
        }
      }
    }

    "run workflow with signals" in { (testEnv: ZTestWorkflowEnvironment) =>
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
            _            <- (signalWorkflow.echoServer _).start("ECHO")
            workflowStub <- client.newUntypedWorkflowStub(workflowId)
            progress <- workflowStub
                          .query0((_: SignalWorkflow).getProgress)
                          .run
            _ <- workflowStub.signal(
                   ZSignal.signal(signalWorkflow.echo _)
                 )(zinput("Hello!"))
            progress2 <- workflowStub
                           .query0((_: SignalWorkflow).getProgress)
                           .run
                           .repeatWhile(_.isEmpty)
            result <- workflowStub.result[String]
          } yield {
            progress should be(empty)
            progress2 should contain("Hello!")
            result shouldEqual "ECHO Hello!"
          }
        }
      }
    }

    "run workflow with successful sagas" in { (testEnv: ZTestWorkflowEnvironment) =>
      val taskQueue = "saga-queue"
      val client    = testEnv.workflowClient

      val successFunc = (_: String, _: BigDecimal) => Right(Done())

      val expectedResult = BigDecimal(5.0)

      testEnv
        .newWorker(taskQueue, options = ZWorkerOptions.default)
        .addWorkflow[SagaWorkflowImpl]
        .fromClass
        .addActivityImplementation(new TransferActivityImpl(successFunc, successFunc))

      val workflowId = UUID.randomUUID().toString

      withWorkflow {
        testEnv.use() {
          for {
            signalWorkflow <- client
                                .newWorkflowStub[SagaWorkflow]
                                .withTaskQueue(taskQueue)
                                .withWorkflowId(workflowId)
                                .withWorkflowRunTimeout(10.seconds)
                                .build
            result <- (signalWorkflow.transfer _).execute(
                        TransferCommand("from", "to", expectedResult)
                      )
          } yield result.value shouldEqual expectedResult
        }
      }
    }

    "run workflow with saga compensations" in { (testEnv: ZTestWorkflowEnvironment) =>
      val taskQueue = "saga-queue"
      val client    = testEnv.workflowClient
      val From      = "from"
      val To        = "to"

      val compensated  = new AtomicBoolean(false)
      val error        = Error("fraud")
      val withdrawFunc = (_: String, _: BigDecimal) => Right(Done())
      val depositFunc: (String, BigDecimal) => Either[fixture.Error, Done] = {
        case (From, _) =>
          compensated.set(true)
          Right(Done())
        case _ =>
          Left(error)
      }

      val amount = BigDecimal(5.0)

      testEnv
        .newWorker(taskQueue, options = ZWorkerOptions.default)
        .addWorkflow[SagaWorkflowImpl]
        .fromClass
        .addActivityImplementation(new TransferActivityImpl(depositFunc, withdrawFunc))

      val workflowId = UUID.randomUUID().toString

      withWorkflow {
        testEnv.use() {
          for {
            sagaWorkflow <- client
                              .newWorkflowStub[SagaWorkflow]
                              .withTaskQueue(taskQueue)
                              .withWorkflowId(workflowId)
                              .withWorkflowRunTimeout(10.seconds)
                              .build
            result <- (sagaWorkflow.transfer _).execute(
                        TransferCommand(From, To, amount)
                      )
          } yield {
            result.left.value shouldEqual error
            assert(compensated.get())
          }
        }
      }
    }

    "run workflow with promise" in { (testEnv: ZTestWorkflowEnvironment) =>
      val taskQueue = "promise-queue"
      val client    = testEnv.workflowClient

      val order = ListBuffer.empty[(String, Int)]
      val fooFunc = (x: Int) => {
        println(s"foo($x)")
        order += ("foo" -> x)
        x
      }
      val barFunc = (x: Int) => {
        println(s"bar($x)")
        order += ("bar" -> x)
        x
      }

      testEnv
        .newWorker(taskQueue, options = ZWorkerOptions.default)
        .addWorkflow[PromiseWorkflowImpl]
        .fromClass
        .addActivityImplementation(new PromiseActivityImpl(fooFunc, barFunc))

      val x = 2
      val y = 3

      withWorkflow {
        testEnv.use() {
          val tests = for {
            workflowId <- UIO.effectTotal(UUID.randomUUID().toString)
            promiseWorkflow <- client
                                 .newWorkflowStub[PromiseWorkflow]
                                 .withTaskQueue(taskQueue)
                                 .withWorkflowId(workflowId)
                                 .withWorkflowRunTimeout(10.seconds)
                                 .build

            result <- (promiseWorkflow.fooBar _).execute(x, y)
          } yield result shouldEqual x + y

          tests.repeatN(19).as {
            val actualResult = order.toList

            println(actualResult.toString)
            actualResult should have size 40
            actualResult should not equal List.fill(20)(List("foo" -> x, "bar" -> y)).flatten
          }
        }
      }
    }
  }

  private def withWorkflow[E, A](f: ZIO[ZEnv, TemporalError[E], A]): Task[A] =
    f.provideLayer(zio.ZEnv.live)
      .mapError(e => new RuntimeException(s"IO failed with $e"))
}
