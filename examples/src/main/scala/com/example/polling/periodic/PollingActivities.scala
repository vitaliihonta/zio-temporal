package com.example.polling.periodic

import zio._
import zio.temporal._
import zio.temporal.activity._

@activityInterface
trait PollingActivities {
  def doPoll(): String
}

object PeriodicPollingActivityImpl {
  val make: URLayer[TestService with ZActivityRunOptions[Any], PollingActivities] =
    ZLayer.fromFunction(new PeriodicPollingActivityImpl(_: TestService)(_: ZActivityRunOptions[Any]))
}

class PeriodicPollingActivityImpl(testService: TestService)(implicit options: ZActivityRunOptions[Any])
    extends PollingActivities {
  override def doPoll(): String =
    ZActivity.run {
      testService.getServiceResult
    }
}
