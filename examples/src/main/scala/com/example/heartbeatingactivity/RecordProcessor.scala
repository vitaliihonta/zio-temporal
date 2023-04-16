package com.example.heartbeatingactivity

import zio._

/** A helper class that implements record processing. */
trait RecordProcessor {

  /** Processes a single record.
    *
    * @param record
    *   record to process
    */
  def processRecord(record: SingleRecord): Task[Unit]
}

object RecordProcessorImpl {
  val make: ULayer[RecordProcessor] = ZLayer.succeed(new RecordProcessorImpl)
}

class RecordProcessorImpl extends RecordProcessor {
  override def processRecord(record: SingleRecord): Task[Unit] = {
    for {
      randomDelay <- ZIO.randomWith(_.nextIntBetween(100, 500))
      _           <- ZIO.sleep(randomDelay.millis)
      _           <- ZIO.logInfo(s"Processed record=$record")
    } yield ()
  }
}
