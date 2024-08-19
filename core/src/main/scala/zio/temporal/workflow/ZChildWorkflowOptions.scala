package zio.temporal.workflow

import io.temporal.api.enums.v1.{ParentClosePolicy, WorkflowIdReusePolicy}
import io.temporal.common.VersioningIntent
import io.temporal.common.context.ContextPropagator
import io.temporal.workflow.{ChildWorkflowCancellationType, ChildWorkflowOptions}
import zio._
import zio.temporal.{ZRetryOptions, ZSearchAttribute, ZSearchAttributes}
import scala.jdk.CollectionConverters._

final case class ZChildWorkflowOptions private[zio] (
  workflowId:               String,
  namespace:                Option[String],
  workflowIdReusePolicy:    Option[WorkflowIdReusePolicy],
  workflowRunTimeout:       Option[Duration],
  workflowExecutionTimeout: Option[Duration],
  workflowTaskTimeout:      Option[Duration],
  taskQueue:                Option[String],
  retryOptions:             Option[ZRetryOptions],
  parentClosePolicy:        Option[ParentClosePolicy],
  memo:                     Map[String, AnyRef],
  searchAttributes:         Option[ZSearchAttributes],
  contextPropagators:       List[ContextPropagator],
  cancellationType:         Option[ChildWorkflowCancellationType],
  versioningIntent:         Option[VersioningIntent],
  private val javaOptionsCustomization: ChildWorkflowOptions.Builder => ChildWorkflowOptions.Builder) {

  /** Workflow id to use when starting. Prefer assigning business meaningful ids if possible.
    */
  def withWorkflowId(value: String): ZChildWorkflowOptions =
    copy(workflowId = value)

  /** Specifies server behavior if a completed workflow with the same id exists. Note that under no conditions Temporal
    * allows two workflows with the same namespace and workflow id run simultaneously. <li>
    *
    * <ul> AllowDuplicateFailedOnly is a default value. It means that workflow can start if previous run failed or was
    * canceled or terminated. </ul>
    *
    * <ul> AllowDuplicate allows new run independently of the previous run closure status. </ul>
    *
    * <ul> RejectDuplicate doesn't allow new run independently of the previous run closure status. </ul>
    */
  def withWorkflowIdReusePolicy(value: WorkflowIdReusePolicy): ZChildWorkflowOptions =
    copy(workflowIdReusePolicy = Some(value))

  /** The time after which child workflow run is automatically terminated by Temporal service with
    * CHILD_WORKFLOW_EXECUTION_TIMED_OUT status. <br> Parent workflow receives
    * [[zio.temporal.failure.ChildWorkflowFailure]] exception with [[zio.temporal.failure.TimeoutFailure]] cause from
    * the child's [[ZAsync]] if this happens.
    *
    * <p>When a workflow reaches Workflow Run Timeout, it can't make any progress after that. Do not rely on this
    * timeout in workflow implementation or business logic. This timeout is not designed to be handled in workflow code
    * to perform any logic in case of timeout. Consider using workflow timers instead.
    *
    * <p>If you catch yourself setting this timeout to very small values, you're likely using it wrong.
    *
    * <p>Example: If Workflow Run Timeout is 30 seconds and the network was unavailable for 1 minute, workflows that
    * were scheduled before the network blip will never have a chance to make progress or react, and will be terminated.
    * <br> A timer that is scheduled in the workflow code using [[ZWorkflow.newTimer]] will handle this situation
    * gracefully. A workflow with such a timer will start after the network blip. If it started before the network blip
    * and the timer fires during the network blip, it will get delivered after connectivity is restored and the workflow
    * will be able to resume.
    */
  def withWorkflowRunTimeout(value: Duration): ZChildWorkflowOptions =
    copy(workflowRunTimeout = Some(value))

  /** The time after which child workflow execution (which includes run retries and continue as new) is automatically
    * terminated by Temporal service with WORKFLOW_EXECUTION_TIMED_OUT status. <br> Parent workflow receives
    * [[zio.temporal.failure.ChildWorkflowFailure]] exception with [[zio.temporal.failure.TimeoutFailure]] cause from
    * the child's [[ZAsync]] if this happens.
    *
    * <p>When a workflow reaches Workflow Execution Timeout, it can't make any progress after that. Do not rely on this
    * timeout in workflow implementation or business logic. This timeout is not designed to be handled in workflow code
    * to perform any logic in case of timeout. Consider using workflow timers instead.
    *
    * <p>If you catch yourself setting this timeout to very small values, you're likely using it wrong.
    *
    * <p>Example: If Workflow Execution Timeout is 30 seconds and the network was unavailable for 1 minute, workflows
    * that were scheduled before the network blip will never have a chance to make progress or react, and will be
    * terminated. <br> A timer that is scheduled in the workflow code using [[ZWorkflow.newTimer]] will handle this
    * situation gracefully. A workflow with such a timer will start after the network blip. If it started before the
    * network blip and the timer fires during the network blip, it will get delivered after connectivity is restored and
    * the workflow will be able to resume.
    */
  def withWorkflowExecutionTimeout(value: Duration): ZChildWorkflowOptions =
    copy(workflowExecutionTimeout = Some(value))

  /** Maximum execution time of a single workflow task. Default is 10 seconds. Maximum accepted value is 60 seconds.
    */
  def withWorkflowTaskTimeout(value: Duration): ZChildWorkflowOptions =
    copy(workflowTaskTimeout = Some(value))

  /** Task queue to use for workflow tasks. It should match a task queue specified when creating a
    * [[zio.temporal.worker.ZWorker]] that hosts the workflow code.
    */
  def withTaskQueue(value: String): ZChildWorkflowOptions =
    copy(taskQueue = Some(value))

  /** RetryOptions that define how child workflow is retried in case of failure. Default is null which is no reties.
    */
  def withRetryOptions(value: ZRetryOptions): ZChildWorkflowOptions =
    copy(retryOptions = Some(value))

  /** Specifies how this workflow reacts to the death of the parent workflow. */
  def withParentClosePolicy(value: ParentClosePolicy): ZChildWorkflowOptions =
    copy(parentClosePolicy = Some(value))

  /** Specifies additional non-indexed information in result of list workflow. The type of value can be any object that
    * are serializable by [[io.temporal.common.converter.DataConverter]]
    */
  def withMemo(values: (String, AnyRef)*): ZChildWorkflowOptions =
    withMemo(values.toMap)

  /** Specifies additional non-indexed information in result of list workflow. The type of value can be any object that
    * are serializable by [[io.temporal.common.converter.DataConverter]]
    */
  def withMemo(values: Map[String, AnyRef]): ZChildWorkflowOptions =
    copy(memo = values)

  /** Specifies Search Attributes that will be attached to the Workflow. Search Attributes are additional indexed
    * information attributed to workflow and used for search and visibility.
    *
    * <p>The search attributes can be used in query of List/Scan/Count workflow APIs. The key and its value type must be
    * registered on Temporal server side.
    */
  def withSearchAttributes(values: Map[String, ZSearchAttribute]): ZChildWorkflowOptions =
    copy(
      searchAttributes = Some(
        ZSearchAttributes.fromJava(
          ZSearchAttribute.toJavaSearchAttributes(values)
        )
      )
    )

  /** Specifies Search Attributes that will be attached to the Workflow. Search Attributes are additional indexed
    * information attributed to workflow and used for search and visibility.
    *
    * <p>The search attributes can be used in query of List/Scan/Count workflow APIs. The key and its value type must be
    * registered on Temporal server side.
    */
  def withSearchAttributes(values: ZSearchAttributes): ZChildWorkflowOptions =
    copy(searchAttributes = Some(values))

  /** Specifies the list of context propagators to use during this workflow. */
  def withContextPropagators(values: ContextPropagator*): ZChildWorkflowOptions =
    withContextPropagators(values.toList)

  /** Specifies the list of context propagators to use during this workflow. */
  def withContextPropagators(values: List[ContextPropagator]): ZChildWorkflowOptions =
    copy(contextPropagators = values)

  /** In case of a child workflow cancellation it fails with a [[zio.temporal.failure.CanceledFailure]]. The type
    * defines at which point the exception is thrown.
    */
  def withCancellationType(value: ChildWorkflowCancellationType): ZChildWorkflowOptions =
    copy(cancellationType = Some(value))

  /** Specifies whether this child workflow should run on a worker with a compatible Build Id or not. See the variants
    * of [[VersioningIntent]].
    */
  def withVersioningIntent(value: VersioningIntent): ZChildWorkflowOptions =
    copy(versioningIntent = Some(value))

  /** Allows to specify options directly on the java SDK's [[ChildWorkflowOptions]]. Use it in case an appropriate
    * `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ChildWorkflowOptions.Builder => ChildWorkflowOptions.Builder
  ): ZChildWorkflowOptions = copy(javaOptionsCustomization = f)

  /** Convert to Java SDK's [[ChildWorkflowOptions]]
    */
  def toJava: ChildWorkflowOptions = {
    val builder = ChildWorkflowOptions.newBuilder()

    builder.setWorkflowId(workflowId)
    namespace.foreach(builder.setNamespace)
    workflowIdReusePolicy.foreach(builder.setWorkflowIdReusePolicy)
    workflowRunTimeout.foreach(builder.setWorkflowRunTimeout)
    workflowExecutionTimeout.foreach(builder.setWorkflowExecutionTimeout)
    workflowTaskTimeout.foreach(builder.setWorkflowTaskTimeout)
    taskQueue.foreach(builder.setTaskQueue)
    retryOptions.foreach(o => builder.setRetryOptions(o.toJava))
    parentClosePolicy.foreach(builder.setParentClosePolicy)
    builder.setMemo(memo.asJava)
    searchAttributes.foreach(s => builder.setTypedSearchAttributes(s.toJava))
    builder.setContextPropagators(contextPropagators.asJava)
    cancellationType.foreach(builder.setCancellationType)
    versioningIntent.foreach(builder.setVersioningIntent)

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = {
    s"ZChildWorkflowOptions(" +
      s"workflowId=$workflowId" +
      s", namespace=$namespace" +
      s", workflowIdReusePolicy=$workflowIdReusePolicy" +
      s", workflowRunTimeout=$workflowRunTimeout" +
      s", workflowExecutionTimeout=$workflowExecutionTimeout" +
      s", workflowTaskTimeout=$workflowTaskTimeout" +
      s", taskQueue=$taskQueue" +
      s", retryOptions=$retryOptions" +
      s", parentClosePolicy=$parentClosePolicy" +
      s", memo=$memo" +
      s", searchAttributes=$searchAttributes" +
      s", contextPropagators=$contextPropagators" +
      s", cancellationType=$cancellationType" +
      s", versioningIntent=$versioningIntent" +
      s")"
  }
}

object ZChildWorkflowOptions {

  /** Workflow id to use when starting. Prefer assigning business meaningful ids if possible.
    */
  def withWorkflowId(value: String): ZChildWorkflowOptions =
    ZChildWorkflowOptions(
      workflowId = value,
      namespace = None,
      workflowIdReusePolicy = None,
      workflowRunTimeout = None,
      workflowExecutionTimeout = None,
      workflowTaskTimeout = None,
      taskQueue = None,
      retryOptions = None,
      parentClosePolicy = None,
      memo = Map.empty,
      searchAttributes = None,
      contextPropagators = Nil,
      cancellationType = None,
      versioningIntent = None,
      javaOptionsCustomization = identity
    )

  /** Constructs workflow options from Java SDK's [[ChildWorkflowOptions]] */
  def fromJava(options: ChildWorkflowOptions): ZChildWorkflowOptions =
    ZChildWorkflowOptions(
      workflowId = options.getWorkflowId,
      namespace = Option(options.getNamespace),
      workflowIdReusePolicy = Option(options.getWorkflowIdReusePolicy),
      workflowRunTimeout = Option(options.getWorkflowRunTimeout),
      workflowExecutionTimeout = Option(options.getWorkflowExecutionTimeout),
      workflowTaskTimeout = Option(options.getWorkflowTaskTimeout),
      taskQueue = Option(options.getTaskQueue),
      retryOptions = Option(options.getRetryOptions).map(ZRetryOptions.fromJava),
      parentClosePolicy = Option(options.getParentClosePolicy),
      memo = Option(options.getMemo).map(_.asScala.toMap).getOrElse(Map.empty),
      searchAttributes = Option(options.getTypedSearchAttributes).map(ZSearchAttributes.fromJava),
      contextPropagators = Option(options.getContextPropagators).toList.flatMap(_.asScala.toList),
      cancellationType = Option(options.getCancellationType),
      versioningIntent = Option(options.getVersioningIntent),
      javaOptionsCustomization = identity
    )
}
