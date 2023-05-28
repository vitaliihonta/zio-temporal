package com.example.hello.misc.generics

import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.workflow.*
import zio.temporal.worker.*
import scala.reflect.ClassTag

// NOTE: compiles, but doesn't work because 'Input' is erased.
// Therefore, jackson can't pick up the proper deserializer
trait ParameterizedChildWorkflow[Input] {
  @workflowMethod
  def childTask(input: Input): Unit
}

trait ParameterizedWorkflow[Input] {
  @workflowMethod
  def parentTask(input: Input): Unit
}

abstract class DelegatingParameterizedWorkflow[
  Input,
  ChildInput,
  ChildWorkflow <: ParameterizedChildWorkflow[ChildInput]: IsWorkflow: ClassTag]
    extends ParameterizedWorkflow[Input] {

  protected def constructChildInput(input: Input, randomData: Int): ChildInput

  private val logger         = ZWorkflow.makeLogger
  private val thisWorkflowId = ZWorkflow.info.workflowId

  override def parentTask(input: Input): Unit = {
    val someData = List(1, 2, 3)
    logger.info("Creating inputs...")
    val inputTasks = someData.map { randomData =>
      randomData -> constructChildInput(input, randomData)
    }

    logger.info("Creating child workflows...")
    // Create multiple parallel child workflows
    val taskRuns = ZAsync.foreachParDiscard(inputTasks) { case (randomData, input) =>
      val child = ZWorkflow
        .newChildWorkflowStub[ChildWorkflow]
        .withWorkflowId(s"$thisWorkflowId/child/$randomData")
        .build

      logger.info(s"Starting child workflow input=$input...")
      ZChildWorkflowStub.executeAsync(
        // TODO: make it work
        child.childTask(input)
      )
    }

    // Wait until completed
    taskRuns.run.getOrThrow
  }
}

case class CocaColaChildInput(kind: String)
case class PepsiChildInput(kind: String, volume: Int)

@workflowInterface
trait CocaColaChildWorkflow extends ParameterizedChildWorkflow[CocaColaChildInput]

class CocaColaChildWorkflowImpl extends CocaColaChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: CocaColaChildInput): Unit = {
    logger.info(s"Providing with Coca Cola ${input.kind}")
  }
}

@workflowInterface
trait PepsiChildWorkflow extends ParameterizedChildWorkflow[PepsiChildInput]

class PepsiChildWorkflowImpl extends PepsiChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: PepsiChildInput): Unit = {
    logger.info(s"Providing with Pepsi ${input.kind} (${input}L)")
  }
}

case class CocaColaInput(kind: String)
case class PepsiInput(kind: String)

@workflowInterface
trait CocaColaWorkflow extends ParameterizedWorkflow[CocaColaInput]

class CocaColaWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      CocaColaInput,
      CocaColaChildInput,
      CocaColaChildWorkflow
    ]
    with CocaColaWorkflow {

  override protected def constructChildInput(input: CocaColaInput, randomData: Int): CocaColaChildInput =
    CocaColaChildInput(input.kind)
}

@workflowInterface
trait PepsiWorkflow extends ParameterizedWorkflow[PepsiInput]

class PepsiWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      PepsiInput,
      PepsiChildInput,
      PepsiChildWorkflow
    ]
    with PepsiWorkflow {

  override protected def constructChildInput(input: PepsiInput, randomData: Int): PepsiChildInput =
    PepsiChildInput(input.kind, randomData)
}

object ParameterizedWorkflowMain extends ZIOAppDefault {
  val TaskQueue = "parameterized"

  private def runWorkflow[Input](
    stub:  ZWorkflowStub.Of[ParameterizedWorkflow[Input]]
  )(input: Input
  ): TemporalIO[Unit] = {
    ZIO.logInfo("Executing parameterized workflow") *>
      // TODO: make it work
      ZWorkflowStub.execute(stub.parentTask(input))
  }

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflow =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[CocaColaWorkflow].from(new CocaColaWorkflowImpl) @@
        ZWorker.addWorkflow[CocaColaChildWorkflow].from(new CocaColaChildWorkflowImpl) @@
        ZWorker.addWorkflow[PepsiWorkflow].from(new PepsiWorkflowImpl) @@
        ZWorker.addWorkflow[PepsiChildWorkflow].from(new PepsiChildWorkflowImpl)

    val flow = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        uuid <- ZIO.randomWith(_.nextUUID)
        cocaCola <- client
                      .newWorkflowStub[CocaColaWorkflow]
                      .withTaskQueue(TaskQueue)
                      .withWorkflowId(s"coca-cola/$uuid")
                      .build

        pepsi <- client
                   .newWorkflowStub[PepsiWorkflow]
                   .withTaskQueue(TaskQueue)
                   .withWorkflowId(s"pepsi/$uuid")
                   .build

        _ <- runWorkflow(cocaCola)(CocaColaInput("zero"))
               .zip(runWorkflow(pepsi)(PepsiInput("classic")))
               .unit

        _ <- ZIO.logInfo("Executed both!")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflow
      _ <- ZWorkflowServiceStubs.setup()
      _ <- ZWorkerFactory.setup
      _ <- flow
    } yield ()

    program
      .provideSome[Scope](
        // temporal
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
        // options
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make
      )
  }
}
