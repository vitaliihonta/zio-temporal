package com.example.generics.jackson_ser

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import zio._
import zio.logging.backend.SLF4J
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._
import scala.reflect.ClassTag

// NOTE: jackson (de)serialization won't work without additional annotations
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ChildWorkflowInput.Soda], name = "Soda"),
    new JsonSubTypes.Type(value = classOf[ChildWorkflowInput.Juice], name = "Juice")
  )
)
sealed trait ChildWorkflowInput
object ChildWorkflowInput {

  case class Soda(kind: String)               extends ChildWorkflowInput
  case class Juice(kind: String, volume: Int) extends ChildWorkflowInput
}

// NOTE: temporal won't deserialize correctly without the upper-bound type
trait ParameterizedChildWorkflow[Input <: ChildWorkflowInput] {
  @workflowMethod
  def childTask(input: Input): Unit
}

// NOTE: jackson (de)serialization won't work without additional annotations
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[WorkflowInput.Soda], name = "Soda"),
    new JsonSubTypes.Type(value = classOf[WorkflowInput.Juice], name = "Juice")
  )
)
sealed trait WorkflowInput
object WorkflowInput {

  case class Soda(kind: String)  extends WorkflowInput
  case class Juice(kind: String) extends WorkflowInput
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
      val child = ZWorkflow.newChildWorkflowStub[ChildWorkflow](
        ZChildWorkflowOptions.withWorkflowId(s"$thisWorkflowId/child/$randomData")
      )

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
trait SodaChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowInput.Soda]

class SodaChildWorkflowImpl extends SodaChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowInput.Soda): Unit = {
    logger.info(s"Providing with soda: ${input.kind}")
  }
}

@workflowInterface
trait JuiceChildWorkflow extends ParameterizedChildWorkflow[ChildWorkflowInput.Juice]

class JuiceChildWorkflowImpl extends JuiceChildWorkflow {
  private val logger = ZWorkflow.makeLogger

  override def childTask(input: ChildWorkflowInput.Juice): Unit = {
    logger.info(s"Providing with ${input.kind} juice (${input.volume}L)")
  }
}

@workflowInterface
trait SodaWorkflow extends ParameterizedWorkflow[WorkflowInput.Soda]

class SodaWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowInput.Soda,
      ChildWorkflowInput.Soda,
      SodaChildWorkflow
    ]
    with SodaWorkflow {

  override protected def constructChildInput(
    input:      WorkflowInput.Soda,
    randomData: Int
  ): ChildWorkflowInput.Soda =
    ChildWorkflowInput.Soda(input.kind)
}

@workflowInterface
trait JuiceWorkflow extends ParameterizedWorkflow[WorkflowInput.Juice]

class JuiceWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      WorkflowInput.Juice,
      ChildWorkflowInput.Juice,
      JuiceChildWorkflow
    ]
    with JuiceWorkflow {

  override protected def constructChildInput(
    input:      WorkflowInput.Juice,
    randomData: Int
  ): ChildWorkflowInput.Juice =
    ChildWorkflowInput.Juice(input.kind, randomData)
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
        ZWorker.addWorkflow[SodaWorkflow].from(new SodaWorkflowImpl) @@
        ZWorker.addWorkflow[SodaChildWorkflow].from(new SodaChildWorkflowImpl) @@
        ZWorker.addWorkflow[JuiceWorkflow].from(new JuiceWorkflowImpl) @@
        ZWorker.addWorkflow[JuiceChildWorkflow].from(new JuiceChildWorkflowImpl)

    val flow = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        uuid <- ZIO.randomWith(_.nextUUID)
        sodaWf <- client.newWorkflowStub[SodaWorkflow](
                    ZWorkflowOptions
                      .withWorkflowId(s"jackson-soda/$uuid")
                      .withTaskQueue(TaskQueue)
                  )

        juiceWf <- client.newWorkflowStub[JuiceWorkflow](
                     ZWorkflowOptions
                       .withWorkflowId(s"jackson-juice/$uuid")
                       .withTaskQueue(TaskQueue)
                   )

        _ <- runWorkflow(sodaWf)(WorkflowInput.Soda("coke"))
               .zip(runWorkflow(juiceWf)(WorkflowInput.Juice("orange")))
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
