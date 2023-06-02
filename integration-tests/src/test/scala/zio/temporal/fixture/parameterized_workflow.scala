package zio.temporal.fixture

import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}
import zio.temporal.*
import zio.temporal.workflow.*
import scala.reflect.ClassTag

case class ParameterizedWorkflowOutput(message: String)

// NOTE: jackson (de)serialization won't work without additional annotations
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ParameterizedChildWorkflowInput.CocaCola], name = "CocaCola"),
    new JsonSubTypes.Type(value = classOf[ParameterizedChildWorkflowInput.Pepsi], name = "Pepsi")
  )
)
sealed trait ParameterizedChildWorkflowInput
object ParameterizedChildWorkflowInput {

  case class CocaCola(kind: String)           extends ParameterizedChildWorkflowInput
  case class Pepsi(kind: String, volume: Int) extends ParameterizedChildWorkflowInput
}

// NOTE: temporal won't deserialize correctly without the lower-bound type
// TODO: add warning about the missing type bound
trait ParameterizedChildWorkflow[Input <: ParameterizedChildWorkflowInput] {
  @workflowMethod
  def childTask(input: Input): ParameterizedWorkflowOutput
}

// NOTE: jackson (de)serialization won't work without additional annotations
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[ParameterizedWorkflowInput.CocaCola], name = "CocaCola"),
    new JsonSubTypes.Type(value = classOf[ParameterizedWorkflowInput.Pepsi], name = "Pepsi")
  )
)
sealed trait ParameterizedWorkflowInput
object ParameterizedWorkflowInput {

  case class CocaCola(kind: String) extends ParameterizedWorkflowInput
  case class Pepsi(kind: String)    extends ParameterizedWorkflowInput
}

// NOTE: temporal won't deserialize correctly without the lower-bound type
trait ParameterizedWorkflow[Input <: ParameterizedWorkflowInput] {
  @workflowMethod
  def parentTask(input: Input): List[ParameterizedWorkflowOutput]
}

abstract class DelegatingParameterizedWorkflow[
  Input <: ParameterizedWorkflowInput,
  ChildInput <: ParameterizedChildWorkflowInput,
  ChildWorkflow <: ParameterizedChildWorkflow[ChildInput]: IsWorkflow: ClassTag]
    extends ParameterizedWorkflow[Input] {

  protected def constructChildInput(input: Input, randomData: Int): ChildInput

  private val logger         = ZWorkflow.makeLogger
  private val thisWorkflowId = ZWorkflow.info.workflowId

  override def parentTask(input: Input): List[ParameterizedWorkflowOutput] = {
    val someData = List(1, 2, 3)
    logger.info("Creating inputs...")
    val inputTasks = someData.map { randomData =>
      randomData -> constructChildInput(input, randomData)
    }

    logger.info("Creating child workflows...")
    // Create multiple parallel child workflows
    val taskRuns = ZAsync.foreachPar(inputTasks) { case (randomData, input) =>
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
trait CocaColaChildWorkflow extends ParameterizedChildWorkflow[ParameterizedChildWorkflowInput.CocaCola]

class CocaColaChildWorkflowImpl extends CocaColaChildWorkflow {
  override def childTask(input: ParameterizedChildWorkflowInput.CocaCola): ParameterizedWorkflowOutput = {
    ParameterizedWorkflowOutput(s"Providing with Coca Cola ${input.kind}")
  }
}

@workflowInterface
trait PepsiChildWorkflow extends ParameterizedChildWorkflow[ParameterizedChildWorkflowInput.Pepsi]

class PepsiChildWorkflowImpl extends PepsiChildWorkflow {
  override def childTask(input: ParameterizedChildWorkflowInput.Pepsi): ParameterizedWorkflowOutput = {
    ParameterizedWorkflowOutput(s"Providing with Pepsi ${input.kind} (${input.volume}L)")
  }
}

@workflowInterface
trait CocaColaWorkflow extends ParameterizedWorkflow[ParameterizedWorkflowInput.CocaCola]

class CocaColaWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      ParameterizedWorkflowInput.CocaCola,
      ParameterizedChildWorkflowInput.CocaCola,
      CocaColaChildWorkflow
    ]
    with CocaColaWorkflow {

  override protected def constructChildInput(
    input:      ParameterizedWorkflowInput.CocaCola,
    randomData: Int
  ): ParameterizedChildWorkflowInput.CocaCola =
    ParameterizedChildWorkflowInput.CocaCola(input.kind)
}

@workflowInterface
trait PepsiWorkflow extends ParameterizedWorkflow[ParameterizedWorkflowInput.Pepsi]

class PepsiWorkflowImpl
    extends DelegatingParameterizedWorkflow[
      ParameterizedWorkflowInput.Pepsi,
      ParameterizedChildWorkflowInput.Pepsi,
      PepsiChildWorkflow
    ]
    with PepsiWorkflow {

  override protected def constructChildInput(
    input:      ParameterizedWorkflowInput.Pepsi,
    randomData: Int
  ): ParameterizedChildWorkflowInput.Pepsi =
    ParameterizedChildWorkflowInput.Pepsi(input.kind, randomData)
}
