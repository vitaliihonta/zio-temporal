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
    "Echo" ->
      ZActivityOptions
        .withStartToCloseTimeout(5.seconds)
        .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1)),
    "ComplexList" ->
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

// Activity with dependencies
object ReporterService {
  val make: ULayer[ReporterService] =
    ZLayer.succeed(new ReporterService)
}
class ReporterService {
  def notify(message: String): Task[Unit] =
    ZIO.logInfo(s"Reporting message: $message")
}

@activityInterface
trait ActivityWithDependencies {
  def reportSomething(n: Int): Unit
}

object ActivityWithDependenciesImpl {
  // the error here is just for testing purposes
  val make: ZLayer[ReporterService with ZActivityRunOptions[Any], Config.Error, ActivityWithDependencies] =
    ZLayer.fromFunction(ActivityWithDependenciesImpl(_: ReporterService)(_: ZActivityRunOptions[Any]))
}

case class ActivityWithDependenciesImpl(
  reporterService: ReporterService
)(implicit options: ZActivityRunOptions[Any])
    extends ActivityWithDependencies {
  override def reportSomething(n: Int): Unit =
    ZActivity.run {
      reporterService.notify(s"given a number=$n")
    }
}

// More workflows
class EvenMoreMultiActivitiesWorkflowImpl extends MultiActivitiesWorkflow {
  private val logger = ZWorkflow.makeLogger

  private val complexTypesActivity = ZWorkflow.newActivityStub[ComplexTypesActivity](
    ZActivityOptions.withStartToCloseTimeout(5.seconds)
  )

  private val activityWithDependencies = ZWorkflow.newActivityStub[ActivityWithDependencies](
    ZActivityOptions.withStartToCloseTimeout(5.seconds)
  )

  private val zioActivity = ZWorkflow.newActivityStub[ZioActivity](
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

    ZActivityStub.execute(
      activityWithDependencies.reportSomething(list.size)
    )

    echoed + s", list=${list.size}"
  }
}
