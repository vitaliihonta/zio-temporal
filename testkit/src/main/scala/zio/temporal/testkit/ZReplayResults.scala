package zio.temporal.testkit

import io.temporal.testing.ReplayResults
import scala.jdk.CollectionConverters._

object ZReplayResults {
  final case class ZReplayError(workflowId: String, exception: Exception)
}

/** Contains workflow reply results
  */
final class ZReplayResults private[zio] (val toJava: ReplayResults) {

  /** @return
    *   all errors reply results has
    */
  val allErrors: List[ZReplayResults.ZReplayError] = {
    toJava
      .allErrors()
      .asScala
      .view
      .map(error => ZReplayResults.ZReplayError(error.workflowId, error.exception))
      .toList
  }

  val hadAnyError: Boolean = allErrors.nonEmpty
}
