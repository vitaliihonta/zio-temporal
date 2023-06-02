package com.example.generics.protobuf_ser

import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.protobuf.ProtobufDataConverter
import zio.temporal.worker.*
import zio.temporal.workflow.*
import scala.reflect.ClassTag

// NOTE: temporal won't deserialize correctly without the lower-bound type
trait ParameterizedChildWorkflow[Input <: ChildWorkflowInput] {
  @workflowMethod
  def childTask(input: Input): Unit
}

// NOTE: temporal won't deserialize correctly without the lower-bound type
trait ParameterizedWorkflow[Input <: WorkflowInput] {
  @workflowMethod
  def parentTask(input: Input): Unit
}

abstract class DelegatingParameterizedWorkflow[
  Input <: WorkflowInput,
  ChildInput <: ChildWorkflowInput,
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
        child.childTask(input)
      )
    }

    // Wait until completed
    taskRuns.run.getOrThrow
  }
}

@workflowInterface
trait CocaColaChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowCocaColaInput]

class CocaColaChildWorkflowImpl extends CocaColaChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowCocaColaInput): Unit = {
    logger.info(s"Providing with Coca Cola ${input.kind}")
  }
}

@workflowInterface
trait PepsiChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowPepsiInput]

class PepsiChildWorkflowImpl extends PepsiChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowPepsiInput): Unit = {
    logger.info(s"Providing with Pepsi ${input.kind} (${input.volume}L)")
  }
}

@workflowInterface
trait CocaColaWorkflow extends ParameterizedWorkflow[WorkflowCocaColaInput]

class CocaColaWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowCocaColaInput,
      ChildWorkflowCocaColaInput,
      CocaColaChildWorkflow
    ]
    with CocaColaWorkflow {

  override protected def constructChildInput(
    input:      WorkflowCocaColaInput,
    randomData: Int
  ): ChildWorkflowCocaColaInput =
    ChildWorkflowCocaColaInput(input.kind)
}

@workflowInterface
trait PepsiWorkflow extends ParameterizedWorkflow[WorkflowPepsiInput]

class PepsiWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowPepsiInput,
      ChildWorkflowPepsiInput,
      PepsiChildWorkflow
    ]
    with PepsiWorkflow {

  override protected def constructChildInput(
    input:      WorkflowPepsiInput,
    randomData: Int
  ): ChildWorkflowPepsiInput =
    ChildWorkflowPepsiInput(input.kind, randomData)
}

object ProtobufParameterizedWorkflowMain extends ZIOAppDefault {
  val TaskQueue = "proto-parameterized"

  private def runWorkflow[Input <: WorkflowInput](
    stub:  ZWorkflowStub.Of[ParameterizedWorkflow[Input]]
  )(input: Input
  ): TemporalIO[Unit] = {
    ZIO.logInfo("Executing parameterized workflow") *>
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
                      .withWorkflowId(s"proto-coca-cola/$uuid")
                      .build

        pepsi <- client
                   .newWorkflowStub[PepsiWorkflow]
                   .withTaskQueue(TaskQueue)
                   .withWorkflowId(s"proto-pepsi/$uuid")
                   .build

        _ <- runWorkflow(cocaCola)(WorkflowCocaColaInput("zero"))
               .zip(runWorkflow(pepsi)(WorkflowPepsiInput("classic")))
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
        ZWorkflowClientOptions.make @@
          ZWorkflowClientOptions.withDataConverter(ProtobufDataConverter.makeAutoLoad()),
        ZWorkerFactoryOptions.make
      )
  }
}
