package com.example.heartbeatingactivity

import zio._

/** Record to process. A real application would add a use case specific data. */
case class SingleRecord(id: Int)

/** Helper class that is used to iterate over a list of records.
  *
  * <p>A specific implementation depends on a use case. For example, it can execute an SQL DB query or read a comma
  * delimited file. More complex use cases would need passing a different type of offset parameter.
  */
trait RecordLoader {

  /** Returns the next record.
    *
    * @param offset
    *   offset of the next record.
    * @return
    *   Record at the offset. Empty optional if offset exceeds the dataset size.
    */
  def getRecord(offset: Int): UIO[Option[SingleRecord]]
}

object RecordLoaderImpl {
  val make: ULayer[RecordLoader] = ZLayer.succeed(new RecordLoaderImpl)
}

class RecordLoaderImpl extends RecordLoader {
  private val RecordCount = 1000

  override def getRecord(offset: Int): UIO[Option[SingleRecord]] = {
    if (offset >= RecordCount) ZIO.none
    else ZIO.some(SingleRecord(offset))
  }
}
