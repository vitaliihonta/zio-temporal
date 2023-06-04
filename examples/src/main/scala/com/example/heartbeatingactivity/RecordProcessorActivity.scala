package com.example.heartbeatingactivity

import zio._
import zio.temporal._
import zio.temporal.activity._

@activityInterface
trait RecordProcessorActivity {

  /** Processes all records in the dataset */
  def processRecords(): Int
}

object RecordProcessorActivityImpl {
  val make: URLayer[RecordLoader with RecordProcessor with ZActivityOptions[Any], RecordProcessorActivity] =
    ZLayer.fromFunction(new RecordProcessorActivityImpl(_: RecordLoader, _: RecordProcessor)(_: ZActivityOptions[Any]))
}

class RecordProcessorActivityImpl(
  recordLoader:     RecordLoader,
  recordProcessor:  RecordProcessor
)(implicit options: ZActivityOptions[Any])
    extends RecordProcessorActivity {

  override def processRecords(): Int = {
    val context = ZActivity.executionContext

    def processLoop(offset: Int): Task[Int] =
      recordLoader.getRecord(offset).flatMap {
        case None => ZIO.succeed(offset)
        case Some(record) =>
          recordProcessor.processRecord(record) *>
            context.heartbeat(offset) *>
            processLoop(offset + 1)
      }

    ZActivity.run {
      for {
        heartbeatDetails <- context.getHeartbeatDetails[Int]
        initialOffset = heartbeatDetails.getOrElse(0)
        _           <- ZIO.logInfo(s"Processing records since offset=$initialOffset")
        finalOffset <- processLoop(initialOffset)
      } yield finalOffset
    }
  }
}
