package com.example.generics.protobuf_ser

import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.protobuf.ProtobufDataConverter
import zio.temporal.worker.*
import zio.temporal.workflow.*
import scala.reflect.ClassTag

// NOTE: temporal won't deserialize correctly without the upper-bound type
trait ParameterizedChildWorkflow[Input <: ChildWorkflowInput] {
  @workflowMethod
  def childTask(input: Input): Unit
}

// NOTE: temporal won't deserialize correctly without the upper-bound type
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
trait SodaChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowSodaInput]

class SodaChildWorkflowImpl extends SodaChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowSodaInput): Unit = {
    logger.info(s"Providing with soda: ${input.kind}")
  }
}

@workflowInterface
trait JuiceChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowJuiceInput]

class JuiceChildWorkflowImpl extends JuiceChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowJuiceInput): Unit = {
    logger.info(s"Providing with ${input.kind} juice (${input.volume}L)")
  }
}

@workflowInterface
trait SodaWorkflow extends ParameterizedWorkflow[WorkflowSodaInput]

class SodaWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowSodaInput,
      ChildWorkflowSodaInput,
      SodaChildWorkflow
    ]
    with SodaWorkflow {

  override protected def constructChildInput(
    input:      WorkflowSodaInput,
    randomData: Int
  ): ChildWorkflowSodaInput =
    ChildWorkflowSodaInput(input.kind)
}

@workflowInterface
trait JuiceWorkflow extends ParameterizedWorkflow[WorkflowJuiceInput]

class JuiceWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowJuiceInput,
      ChildWorkflowJuiceInput,
      JuiceChildWorkflow
    ]
    with JuiceWorkflow {

  override protected def constructChildInput(
    input:      WorkflowJuiceInput,
    randomData: Int
  ): ChildWorkflowJuiceInput =
    ChildWorkflowJuiceInput(input.kind, randomData)
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
        ZWorker.addWorkflow[SodaWorkflow].from(new SodaWorkflowImpl) @@
        ZWorker.addWorkflow[SodaChildWorkflow].from(new SodaChildWorkflowImpl) @@
        ZWorker.addWorkflow[JuiceWorkflow].from(new JuiceWorkflowImpl) @@
        ZWorker.addWorkflow[JuiceChildWorkflow].from(new JuiceChildWorkflowImpl)

    val flow = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        uuid <- ZIO.randomWith(_.nextUUID)
        sodaWf <- client
                    .newWorkflowStub[SodaWorkflow]
                    .withTaskQueue(TaskQueue)
                    .withWorkflowId(s"proto-soda/$uuid")
                    .build

        juiceWf <- client
                     .newWorkflowStub[JuiceWorkflow]
                     .withTaskQueue(TaskQueue)
                     .withWorkflowId(s"proto-juice/$uuid")
                     .build

        _ <- runWorkflow(sodaWf)(WorkflowSodaInput("coke"))
               .zip(runWorkflow(juiceWf)(WorkflowJuiceInput("orange")))
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
