package com.example.generics.jackson_ser

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import scala.reflect.ClassTag

// NOTE: jackson (de)serialization won't work without additional annotations
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ChildWorkflowInput.CocaCola], name = "CocaCola"),
    new JsonSubTypes.Type(value = classOf[ChildWorkflowInput.Pepsi], name = "Pepsi")
  )
)
sealed trait ChildWorkflowInput
object ChildWorkflowInput {

  case class CocaCola(kind: String)           extends ChildWorkflowInput
  case class Pepsi(kind: String, volume: Int) extends ChildWorkflowInput
}

// NOTE: temporal won't deserialize correctly without the lower-bound type
// TODO: add warning about the missing type bound
trait ParameterizedChildWorkflow[Input <: ChildWorkflowInput] {
  @workflowMethod
  def childTask(input: Input): Unit
}

// NOTE: jackson (de)serialization won't work without additional annotations
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[WorkflowInput.CocaCola], name = "CocaCola"),
    new JsonSubTypes.Type(value = classOf[WorkflowInput.Pepsi], name = "Pepsi")
  )
)
sealed trait WorkflowInput
object WorkflowInput {

  case class CocaCola(kind: String) extends WorkflowInput
  case class Pepsi(kind: String)    extends WorkflowInput
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
trait CocaColaChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowInput.CocaCola]

class CocaColaChildWorkflowImpl extends CocaColaChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowInput.CocaCola): Unit = {
    logger.info(s"Providing with Coca Cola ${input.kind}")
  }
}

@workflowInterface
trait PepsiChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowInput.Pepsi]

class PepsiChildWorkflowImpl extends PepsiChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowInput.Pepsi): Unit = {
    logger.info(s"Providing with Pepsi ${input.kind} (${input.volume}L)")
  }
}

@workflowInterface
trait CocaColaWorkflow extends ParameterizedWorkflow[WorkflowInput.CocaCola]

class CocaColaWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowInput.CocaCola,
      ChildWorkflowInput.CocaCola,
      CocaColaChildWorkflow
    ]
    with CocaColaWorkflow {

  override protected def constructChildInput(
    input:      WorkflowInput.CocaCola,
    randomData: Int
  ): ChildWorkflowInput.CocaCola =
    ChildWorkflowInput.CocaCola(input.kind)
}

@workflowInterface
trait PepsiWorkflow extends ParameterizedWorkflow[WorkflowInput.Pepsi]

class PepsiWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowInput.Pepsi,
      ChildWorkflowInput.Pepsi,
      PepsiChildWorkflow
    ]
    with PepsiWorkflow {

  override protected def constructChildInput(
    input:      WorkflowInput.Pepsi,
    randomData: Int
  ): ChildWorkflowInput.Pepsi =
    ChildWorkflowInput.Pepsi(input.kind, randomData)
}

object JacksonParameterizedWorkflowMain extends ZIOAppDefault {
  val TaskQueue = "jackson-parameterized"

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
                      .withWorkflowId(s"jackson-coca-cola/$uuid")
                      .build

        pepsi <- client
                   .newWorkflowStub[PepsiWorkflow]
                   .withTaskQueue(TaskQueue)
                   .withWorkflowId(s"jackson-pepsi/$uuid")
                   .build

        _ <- runWorkflow(cocaCola)(WorkflowInput.CocaCola("zero"))
               .zip(runWorkflow(pepsi)(WorkflowInput.Pepsi("classic")))
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
