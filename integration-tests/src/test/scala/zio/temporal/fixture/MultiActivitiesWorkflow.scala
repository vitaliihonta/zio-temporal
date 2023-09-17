package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._

@workflowInterface
trait MultiActivitiesWorkflow {

  @workflowMethod
  def doSomething(input: String): String
}

class MultiActivitiesWorkflowImpl extends MultiActivitiesWorkflow {
  private val logger = ZWorkflow.makeLogger

  ZWorkflow.applyActivityOptions(
    ZActivityType[ZioActivity] ->
      ZActivityOptions
        .withStartToCloseTimeout(5.seconds)
        .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1)),
    ZActivityType[ComplexTypesActivity] ->
      ZActivityOptions
        .withStartToCloseTimeout(5.seconds)
        .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(3))
  )

  private val zioActivity = ZWorkflow.newActivityStub[ZioActivity](
    ZActivityOptions.withStartToCloseTimeout(5.seconds)
  )

  private val complexTypesActivity = ZWorkflow.newActivityStub[ComplexTypesActivity](
    ZActivityOptions.withStartToCloseTimeout(5.seconds)
  )

  override def doSomething(input: String): String = {
    logger.info("Invoking first activity...")
    val echoed = ZActivityStub.execute(
      zioActivity.echo(input)
    )

    logger.info("Invoking second activity...")
    val list = ZActivityStub.execute(
      complexTypesActivity.complexList
    )

    echoed + s", list=${list.size}"
  }
}
