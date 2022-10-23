package zio.temporal.workflow

import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.client.WorkflowOptions
import io.temporal.common.context.ContextPropagator
import zio.temporal.ZRetryOptions
import zio.temporal.ZSearchAttribute
import zio._
import scala.jdk.CollectionConverters._

/** Represents temporal workflow options
  *
  * @see
  *   [[WorkflowOptions]]
  */
case class ZWorkflowOptions private[zio] (
  workflowIdReusePolicy:                Option[WorkflowIdReusePolicy],
  workflowRunTimeout:                   Option[Duration],
  workflowExecutionTimeout:             Option[Duration],
  retryOptions:                         Option[ZRetryOptions],
  cronSchedule:                         Option[String],
  memo:                                 Map[String, AnyRef],
  searchAttributes:                     Map[String, ZSearchAttribute],
  contextPropagators:                   List[ContextPropagator],
  private val javaOptionsCustomization: WorkflowOptions.Builder => WorkflowOptions.Builder) {

  def withWorkflowIdReusePolicy(value: WorkflowIdReusePolicy): ZWorkflowOptions =
    copy(workflowIdReusePolicy = Some(value))

  def withWorkflowRunTimeout(value: Duration): ZWorkflowOptions =
    copy(workflowRunTimeout = Some(value))

  def withWorkflowExecutionTimeout(value: Duration): ZWorkflowOptions =
    copy(workflowExecutionTimeout = Some(value))

  def withRetryOptions(value: ZRetryOptions): ZWorkflowOptions =
    copy(retryOptions = Some(value))

  def withCronSchedule(value: String): ZWorkflowOptions =
    copy(cronSchedule = Some(value))

  def withMemo(values: (String, AnyRef)*): ZWorkflowOptions =
    copy(memo = values.toMap)

  def withSearchAttributes(values: (String, ZSearchAttribute)*): ZWorkflowOptions =
    copy(searchAttributes = values.toMap)

  def withContextPropagators(values: ContextPropagator*): ZWorkflowOptions =
    copy(contextPropagators = values.toList)

  /** Allows to specify options directly on the java SDK's [[WorkflowOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkflowOptions.Builder => WorkflowOptions.Builder
  ): ZWorkflowOptions =
    copy(javaOptionsCustomization = f)

  def toJava(workflowId: String): WorkflowOptions = {
    val builder = WorkflowOptions.newBuilder().setWorkflowId(workflowId)

    workflowIdReusePolicy.foreach(builder.setWorkflowIdReusePolicy)
    workflowRunTimeout.foreach(t => builder.setWorkflowRunTimeout(t.asJava))
    workflowExecutionTimeout.foreach(t => builder.setWorkflowExecutionTimeout(t.asJava))
    retryOptions.foreach(t => builder.setRetryOptions(t.toJava))
    cronSchedule.foreach(builder.setCronSchedule)

    builder.setMemo(memo.asJava)
    builder.setSearchAttributes(searchAttributes)
    builder.setContextPropagators(contextPropagators.asJava)

    javaOptionsCustomization(builder).build()
  }
}

object ZWorkflowOptions {

  val default: ZWorkflowOptions = new ZWorkflowOptions(
    workflowIdReusePolicy = None,
    workflowRunTimeout = None,
    workflowExecutionTimeout = None,
    retryOptions = None,
    cronSchedule = None,
    memo = Map.empty,
    searchAttributes = Map.empty,
    contextPropagators = Nil,
    javaOptionsCustomization = identity
  )
}
