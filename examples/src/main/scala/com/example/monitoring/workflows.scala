package com.example.monitoring

import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.activity._

@activityInterface
trait SampleActivities {
  def helloMessage(who: String): String

  def byeMessage(who: String): String
}

object SampleActivitiesImpl {
  val make: URLayer[ZActivityRunOptions[Any], SampleActivities] =
    ZLayer.fromFunction(SampleActivitiesImpl()(_: ZActivityRunOptions[Any]))
}

case class SampleActivitiesImpl()(implicit options: ZActivityRunOptions[Any]) extends SampleActivities {
  override def helloMessage(who: String): String =
    ZActivity.run {
      for {
        _ <- ZIO.logInfo(s"Hello to $who")
        _ <- randomDelay()
      } yield s"Hello $who"
    }

  override def byeMessage(who: String): String = {
    ZActivity.run {
      for {
        _ <- ZIO.logInfo(s"Bye $who")
        _ <- randomDelay()
      } yield s"Bye-bye $who"
    }
  }

  private def randomDelay(): UIO[Unit] = {
    Random.nextIntBetween(1, 5).flatMap(n => ZIO.sleep(n.seconds))
  }
}

@workflowInterface
trait SampleWorkflow {
  @workflowMethod
  def greetAndBye(who: String): List[String]
}

class SampleWorkflowImpl extends SampleWorkflow {
  private val sampleActivities = ZWorkflow
    .newActivityStub[SampleActivities](
      ZActivityOptions.withStartToCloseTimeout(5.seconds)
    )

  private val random = ZWorkflow.newRandom

  override def greetAndBye(who: String): List[String] = {
    val greet = ZActivityStub.execute(
      sampleActivities.helloMessage(who)
    )

    ZWorkflow.sleep(random.nextInt(5).seconds)

    val bye = ZActivityStub.execute(
      sampleActivities.byeMessage(who)
    )

    List(greet, bye)
  }
}
