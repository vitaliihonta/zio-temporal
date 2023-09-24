package com.example.heartbeatingactivity

import zio._
import zio.temporal._
import zio.temporal.activity._

@activityInterface
trait ReporterActivity {
  def reportProcessed(numRecords: Int): Unit
}

object ReporterActivityImpl {
  val make: URLayer[ZActivityRunOptions[Any], ReporterActivity] =
    ZLayer.fromFunction(new ReporterActivityImpl()(_: ZActivityRunOptions[Any]))
}

class ReporterActivityImpl()(implicit options: ZActivityRunOptions[Any]) extends ReporterActivity {
  override def reportProcessed(numRecords: Int): Unit = {
    ZActivity.run {
      ZIO.logInfo(s"Processed $numRecords records")
    }
  }
}
