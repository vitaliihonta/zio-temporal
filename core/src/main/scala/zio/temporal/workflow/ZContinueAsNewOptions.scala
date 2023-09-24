package zio.temporal.workflow

import io.temporal.common.VersioningIntent
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.ContinueAsNewOptions
import zio._
import zio.temporal.{ZSearchAttribute, ZSearchAttributes}
import scala.jdk.CollectionConverters._

/** This class contain overrides for continueAsNew call. Every field can be null and it means that the value of the
  * option should be taken from the originating workflow run.
  */
final case class ZContinueAsNewOptions private[zio] (
  workflowRunTimeout:                   Option[Duration],
  taskQueue:                            Option[String],
  workflowTaskTimeout:                  Option[Duration],
  memo:                                 Map[String, AnyRef],
  searchAttributes:                     Option[ZSearchAttributes],
  contextPropagators:                   List[ContextPropagator],
  versioningIntent:                     Option[VersioningIntent],
  private val javaOptionsCustomization: ContinueAsNewOptions.Builder => ContinueAsNewOptions.Builder) {

  /** @see
    *   [[ZWorkflowOptions.withWorkflowRunTimeout]]
    */
  def withWorkflowRunTimeout(value: Duration): ZContinueAsNewOptions =
    copy(workflowRunTimeout = Some(value))

  /** @see
    *   [[ZWorkflowOptions.taskQueue]]
    */
  def withTaskQueue(value: String): ZContinueAsNewOptions =
    copy(taskQueue = Some(value))

  /** @see
    *   [[ZWorkflowOptions.withWorkflowTaskTimeout]]
    */
  def withWorkflowTaskTimeout(value: Duration): ZContinueAsNewOptions =
    copy(workflowTaskTimeout = Some(value))

  /** @see
    *   [[ZWorkflowOptions.withMemo]]
    */
  def withMemo(values: (String, AnyRef)*): ZContinueAsNewOptions =
    withMemo(values.toMap)

  /** @see
    *   [[ZWorkflowOptions.withMemo]]
    */
  def withMemo(values: Map[String, AnyRef]): ZContinueAsNewOptions =
    copy(memo = values)

  /** @see
    *   [[ZWorkflowOptions.withSearchAttributes]]
    */
  def withSearchAttributes(values: Map[String, ZSearchAttribute]): ZContinueAsNewOptions =
    copy(
      searchAttributes = Some(
        ZSearchAttributes.fromJava(
          ZSearchAttribute.toJavaSearchAttributes(values)
        )
      )
    )

  /** @see
    *   [[ZWorkflowOptions.withSearchAttributes]]
    */
  def withSearchAttributes(values: ZSearchAttributes): ZContinueAsNewOptions =
    copy(searchAttributes = Some(values))

  /** @see
    *   [[ZWorkflowOptions.withContextPropagators]]
    */
  def withContextPropagators(values: ContextPropagator*): ZContinueAsNewOptions =
    withContextPropagators(values.toList)

  /** @see
    *   [[ZWorkflowOptions.withContextPropagators]]
    */
  def withContextPropagators(values: List[ContextPropagator]): ZContinueAsNewOptions =
    copy(contextPropagators = values)

  def withVersioningIntent(value: VersioningIntent): ZContinueAsNewOptions =
    copy(versioningIntent = Some(value))

  /** Allows to specify options directly on the java SDK's [[ContinueAsNewOptions]]. Use it in case an appropriate
    * `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ContinueAsNewOptions.Builder => ContinueAsNewOptions.Builder
  ): ZContinueAsNewOptions = copy(javaOptionsCustomization = f)

  /** Converts continue-as-new options into Java SDK's [[ContinueAsNewOptions]]
    */
  def toJava: ContinueAsNewOptions = {
    val builder = ContinueAsNewOptions.newBuilder()

    workflowRunTimeout.foreach(builder.setWorkflowRunTimeout)
    taskQueue.foreach(builder.setTaskQueue)
    workflowTaskTimeout.foreach(builder.setWorkflowTaskTimeout)
    builder.setMemo(memo.asJava)
    searchAttributes.foreach(s => builder.setTypedSearchAttributes(s.toJava))
    builder.setContextPropagators(contextPropagators.asJava)
    versioningIntent.foreach(builder.setVersioningIntent)

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = s"ZContinueAsNewOptions(" +
    s"workflowRunTimeout=$workflowRunTimeout" +
    s", taskQueue=$taskQueue" +
    s", workflowTaskTimeout=$workflowTaskTimeout" +
    s", memo=$memo" +
    s", searchAttributes=$searchAttributes" +
    s", contextPropagators=$contextPropagators" +
    s", versioningIntent=$versioningIntent" +
    s")"
}

object ZContinueAsNewOptions {
  val default: ZContinueAsNewOptions = {
    ZContinueAsNewOptions(
      workflowRunTimeout = None,
      taskQueue = None,
      workflowTaskTimeout = None,
      memo = Map.empty,
      searchAttributes = None,
      contextPropagators = Nil,
      versioningIntent = None,
      javaOptionsCustomization = identity
    )
  }
}
