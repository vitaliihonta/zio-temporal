package zio.temporal.workflow

import io.temporal.client.WorkerBuildIdVersionSets
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

object ZWorkerBuildIdVersionSets {

  /** Represents a set of Build Ids which are compatible with one another. */
  final class CompatibleSet private[zio] (val toJava: WorkerBuildIdVersionSets.CompatibleSet) {

    /** @return
      *   All the Build Ids in the set
      */
    def buildIds: List[String] =
      toJava.getBuildIds.asScala.toList

    /** @return
      *   The default Build Id for this compatible set.
      */
    def defaultBuildId: Option[String] =
      toJava.defaultBuildId().toScala

    override def toString: String = {
      s"ZWorkerBuildIdVersionSets.CompatibleSet(" +
        s"buildIds=${buildIds.mkString("[", ", ", "]")}" +
        s", defaultBuildId=$defaultBuildId" +
        s")"
    }
  }
}

/** Represents the sets of compatible Build Ids associated with a particular task queue.
  * @note
  *   experimental in Java SDK
  */
final class ZWorkerBuildIdVersionSets private[zio] (val toJava: WorkerBuildIdVersionSets) {

  /** @return
    *   the current overall default Build Id for the queue. Returns empty if there are no defined Build Ids.
    */
  def defaultBuildId: Option[String] =
    toJava.defaultBuildId().toScala

  /** @return
    *   the current overall default compatible set for the queue. Returns null if there are no defined Build Ids.
    */
  def defaultSet =
    new ZWorkerBuildIdVersionSets.CompatibleSet(toJava.defaultSet())

  /** @return
    *   All compatible sets for the queue. The last set in the list is the overall default.
    */
  def allSets: List[ZWorkerBuildIdVersionSets.CompatibleSet] =
    toJava.allSets().asScala.map(new ZWorkerBuildIdVersionSets.CompatibleSet(_)).toList

  override def toString: String = {
    s"ZWorkerBuildIdVersionSets(" +
      s"defaultBuildId=$defaultBuildId" +
      s", defaultSet=$defaultSet" +
      s", allSets=${allSets.mkString("[", ", ", "]")}" +
      s")"
  }
}
