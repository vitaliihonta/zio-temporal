package zio.temporal.workflow

import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.client.WorkflowOptions
import io.temporal.common.context.ContextPropagator
import zio.temporal.ZRetryOptions
import zio.temporal.ZSearchAttribute

import scala.compat.java8.DurationConverters.FiniteDurationops
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._

/** Represents temporal workflow options
  *
  * @see
  *   [[WorkflowOptions]]
  */
class ZWorkflowOptions private[zio] (
  val workflowIdReusePolicy:    Option[WorkflowIdReusePolicy],
  val workflowRunTimeout:       Option[FiniteDuration],
  val workflowExecutionTimeout: Option[FiniteDuration],
  val retryOptions:             Option[ZRetryOptions],
  val cronSchedule:             Option[String],
  val memo:                     Map[String, AnyRef],
  val searchAttributes:         Map[String, ZSearchAttribute],
  val contextPropagators:       List[ContextPropagator]) {

  override def toString: String =
    s"ZWorkflowOptions(" +
      s"workflowIdReusePolicy=$workflowIdReusePolicy, " +
      s"workflowRunTimeout=$workflowRunTimeout, " +
      s"workflowExecutionTimeout=$workflowExecutionTimeout, " +
      s"retryOptions=$retryOptions, " +
      s"cronSchedule=$cronSchedule, " +
      s"memo=$memo, " +
      s"searchAttributes=$searchAttributes, " +
      s"contextPropagators=$contextPropagators)"

  def withWorkflowIdReusePolicy(value: WorkflowIdReusePolicy): ZWorkflowOptions =
    copy(_workflowIdReusePolicy = Some(value))

  def withWorkflowRunTimeout(value: FiniteDuration): ZWorkflowOptions =
    copy(_workflowRunTimeout = Some(value))

  def withWorkflowExecutionTimeout(value: FiniteDuration): ZWorkflowOptions =
    copy(_workflowExecutionTimeout = Some(value))

  def withRetryOptions(value: ZRetryOptions): ZWorkflowOptions =
    copy(_retryOptions = Some(value))

  def withCronSchedule(value: String): ZWorkflowOptions =
    copy(_cronSchedule = Some(value))

  def withMemo(values: (String, AnyRef)*): ZWorkflowOptions =
    copy(_memo = values.toMap)

  def withSearchAttributes(values: (String, ZSearchAttribute)*): ZWorkflowOptions =
    copy(_searchAttributes = values.toMap)

  def withContextPropagators(values: ContextPropagator*): ZWorkflowOptions =
    copy(_contextPropagators = values.toList)

  def toJava(workflowId: String): WorkflowOptions = {
    val builder = WorkflowOptions.newBuilder().setWorkflowId(workflowId)

    workflowIdReusePolicy.foreach(builder.setWorkflowIdReusePolicy)
    workflowRunTimeout.foreach(t => builder.setWorkflowRunTimeout(t.toJava))
    workflowExecutionTimeout.foreach(t => builder.setWorkflowExecutionTimeout(t.toJava))
    retryOptions.foreach(t => builder.setRetryOptions(t.toJava))
    cronSchedule.foreach(builder.setCronSchedule)

    builder.setMemo(memo.asJava)
    builder.setSearchAttributes(searchAttributes)
    builder.setContextPropagators(contextPropagators.asJava)

    builder.build()
  }

  private def copy(
    _workflowIdReusePolicy:    Option[WorkflowIdReusePolicy] = workflowIdReusePolicy,
    _workflowRunTimeout:       Option[FiniteDuration] = workflowRunTimeout,
    _workflowExecutionTimeout: Option[FiniteDuration] = workflowExecutionTimeout,
    _retryOptions:             Option[ZRetryOptions] = retryOptions,
    _cronSchedule:             Option[String] = cronSchedule,
    _memo:                     Map[String, AnyRef] = memo,
    _searchAttributes:         Map[String, ZSearchAttribute] = searchAttributes,
    _contextPropagators:       List[ContextPropagator] = contextPropagators
  ): ZWorkflowOptions =
    new ZWorkflowOptions(
      _workflowIdReusePolicy,
      _workflowRunTimeout,
      _workflowExecutionTimeout,
      _retryOptions,
      _cronSchedule,
      _memo,
      _searchAttributes,
      _contextPropagators
    )
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
    contextPropagators = Nil
  )
}
