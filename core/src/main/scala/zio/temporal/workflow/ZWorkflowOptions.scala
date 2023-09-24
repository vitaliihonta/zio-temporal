package zio.temporal.workflow

import io.temporal.api.enums.v1.WorkflowIdReusePolicy
import io.temporal.client.WorkflowOptions
import io.temporal.common.context.ContextPropagator
import zio._
import zio.temporal.{ZRetryOptions, ZSearchAttribute, ZSearchAttributes}
import scala.jdk.CollectionConverters._

/** Options used to configure how a workflow is executed. */
final case class ZWorkflowOptions private[zio] (
  workflowId:                           String,
  taskQueue:                            String,
  workflowIdReusePolicy:                Option[WorkflowIdReusePolicy],
  workflowRunTimeout:                   Option[Duration],
  workflowExecutionTimeout:             Option[Duration],
  workflowTaskTimeout:                  Option[Duration],
  retryOptions:                         Option[ZRetryOptions],
  memo:                                 Map[String, AnyRef],
  searchAttributes:                     Option[ZSearchAttributes],
  contextPropagators:                   List[ContextPropagator],
  disableEagerExecution:                Option[Boolean],
  private val javaOptionsCustomization: WorkflowOptions.Builder => WorkflowOptions.Builder) {

  /** Workflow id to use when starting. Prefer assigning business meaningful ids if possible.
    */
  def withWorkflowId(value: String): ZWorkflowOptions =
    copy(workflowId = value)

  /** Task queue to use for workflow tasks. It should match a task queue specified when creating a
    * [[zio.temporal.worker.ZWorker]] that hosts the workflow code.
    */
  def withTaskQueue(value: String): ZWorkflowOptions =
    copy(taskQueue = value)

  /** Specifies server behavior if a completed workflow with the same id exists. Note that under no conditions Temporal
    * allows two workflows with the same namespace and workflow id run simultaneously.
    *
    * <p>Default value if not set: <b>AllowDuplicate</b>
    *
    * <ul> <li><b>AllowDuplicate</b> allows a new run regardless of the previous run's final status. The previous run
    * still must be closed or the new run will be rejected. <li><b>AllowDuplicateFailedOnly</b> allows a new run if the
    * previous run failed, was canceled, or terminated. <li><b>RejectDuplicate</b> never allows a new run, regardless of
    * the previous run's final status. <li><b>TerminateIfRunning</b> is the same as <b>AllowDuplicate</b>, but if there
    * exists a not-closed run in progress, it will be terminated. </ul>
    */
  def withWorkflowIdReusePolicy(value: WorkflowIdReusePolicy): ZWorkflowOptions =
    copy(workflowIdReusePolicy = Some(value))

  /** The time after which a workflow run is automatically terminated by Temporal service with
    * WORKFLOW_EXECUTION_TIMED_OUT status.
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
  def withWorkflowRunTimeout(timeout: Duration): ZWorkflowOptions =
    copy(workflowRunTimeout = Some(timeout))

  /** The time after which workflow execution (which includes run retries and continue as new) is automatically
    * terminated by Temporal service with WORKFLOW_EXECUTION_TIMED_OUT status.
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
  def withWorkflowExecutionTimeout(timeout: Duration): ZWorkflowOptions =
    copy(workflowExecutionTimeout = Some(timeout))

  /** Maximum execution time of a single Workflow Task. In the majority of cases there is no need to change this
    * timeout. Note that this timeout is not related to the overall Workflow duration in any way. It defines for how
    * long the Workflow can get blocked in the case of a Workflow Worker crash.
    *
    * <p>Default is 10 seconds. Maximum value allowed by the Temporal Server is 1 minute.
    */
  def withWorkflowTaskTimeout(timeout: Duration): ZWorkflowOptions =
    copy(workflowTaskTimeout = Some(timeout))

  /** Specifies retry optiosn for this workflow
    */
  def withRetryOptions(options: ZRetryOptions): ZWorkflowOptions =
    copy(retryOptions = Some(options))

  /** Specifies additional non-indexed information in result of list workflow. The type of value can be any object that
    * are serializable by [[io.temporal.common.converter.DataConverter]]
    */
  def withMemo(values: (String, AnyRef)*): ZWorkflowOptions =
    withMemo(values.toMap)

  /** Specifies additional non-indexed information in result of list workflow. The type of value can be any object that
    * are serializable by [[io.temporal.common.converter.DataConverter]]
    */
  def withMemo(values: Map[String, AnyRef]): ZWorkflowOptions =
    copy(memo = values)

  /** Specifies Search Attributes that will be attached to the Workflow. Search Attributes are additional indexed
    * information attributed to workflow and used for search and visibility.
    *
    * <p>The search attributes can be used in query of List/Scan/Count workflow APIs. The key and its value type must be
    * registered on Temporal server side.
    */
  def withSearchAttributes(values: Map[String, ZSearchAttribute]): ZWorkflowOptions =
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
  def withSearchAttributes(values: ZSearchAttributes): ZWorkflowOptions =
    copy(searchAttributes = Some(values))

  /** This list of context propagators overrides the list specified on [[ZWorkflowClientOptions.contextPropagators]].
    * <br> This method is uncommon, the majority of users should just set [[ZWorkflowClientOptions.contextPropagators]]
    *
    * @param values
    *   specifies the list of overriding context propagators
    */
  def withContextPropagators(values: ContextPropagator*): ZWorkflowOptions =
    withContextPropagators(values.toList)

  /** This list of context propagators overrides the list specified on [[ZWorkflowClientOptions.contextPropagators]].
    * <br> This method is uncommon, the majority of users should just set [[ZWorkflowClientOptions.contextPropagators]]
    *
    * @param values
    *   specifies the list of overriding context propagators
    */
  def withContextPropagators(values: List[ContextPropagator]): ZWorkflowOptions =
    copy(contextPropagators = values)

  /** If [[ZWorkflowClient]] is used to create a [[zio.temporal.worker.ZWorkerFactory]] that is
    *
    * <ul> <li>started <li>has a non-paused worker on the right task queue <li>has available workflow task executor
    * slots </ul>
    *
    * and such a [[ZWorkflowClient]] is used to start a workflow, then the first workflow task could be dispatched on
    * this local worker with the response to the start call if Server supports it. This option can be used to disable
    * this mechanism.
    *
    * @param value
    *   if true, an eager local execution of the workflow task will never be requested even if it is possible.
    */
  def withDisableEagerExecution(value: Boolean): ZWorkflowOptions =
    copy(disableEagerExecution = Some(value))

  /** Allows to specify options directly on the java SDK's [[WorkflowOptions]]. Use it in case an appropriate `withXXX`
    * method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkflowOptions.Builder => WorkflowOptions.Builder
  ): ZWorkflowOptions = copy(javaOptionsCustomization = f)

  /** Convert to Java SDK's [[WorkflowOptions]]
    */
  def toJava: WorkflowOptions = {
    val builder = WorkflowOptions.newBuilder()

    builder.setWorkflowId(workflowId)
    builder.setTaskQueue(taskQueue)
    workflowIdReusePolicy.foreach(builder.setWorkflowIdReusePolicy)
    workflowRunTimeout.foreach(builder.setWorkflowRunTimeout)
    workflowExecutionTimeout.foreach(builder.setWorkflowExecutionTimeout)
    workflowTaskTimeout.foreach(builder.setWorkflowTaskTimeout)
    retryOptions.foreach(o => builder.setRetryOptions(o.toJava))
    builder.setMemo(memo.asJava)
    searchAttributes.foreach(s => builder.setTypedSearchAttributes(s.toJava))
    builder.setContextPropagators(contextPropagators.asJava)
    disableEagerExecution.foreach(builder.setDisableEagerExecution)

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = {
    s"ZWorkflowOptions(" +
      s"workflowId=$workflowId" +
      s", taskQueue=$taskQueue" +
      s", workflowIdReusePolicy=$workflowIdReusePolicy" +
      s", workflowRunTimeout=$workflowRunTimeout" +
      s", workflowExecutionTimeout=$workflowExecutionTimeout" +
      s", workflowTaskTimeout=$workflowTaskTimeout" +
      s", retryOptions=$retryOptions" +
      s", memo=$memo" +
      s", searchAttributes=$searchAttributes" +
      s", contextPropagators=$contextPropagators" +
      s", disableEagerExecution=$disableEagerExecution" +
      s")"
  }
}

object ZWorkflowOptions {

  /** Workflow id to use when starting. Prefer assigning business meaningful ids if possible.
    */
  def withWorkflowId(value: String): SetTaskQueue =
    new SetTaskQueue(value)

  final class SetTaskQueue private[zio] (val workflowId: String) extends AnyVal {

    /** Task queue to use for workflow tasks. It should match a task queue specified when creating a
      * [[zio.temporal.worker.ZWorker]] that hosts the workflow code.
      */
    def withTaskQueue(value: String): ZWorkflowOptions =
      ZWorkflowOptions(
        workflowId = workflowId,
        taskQueue = value,
        workflowIdReusePolicy = None,
        workflowRunTimeout = None,
        workflowExecutionTimeout = None,
        workflowTaskTimeout = None,
        retryOptions = None,
        memo = Map.empty,
        searchAttributes = None,
        contextPropagators = Nil,
        disableEagerExecution = None,
        javaOptionsCustomization = identity
      )
  }

  /** Constructs workflow options from Java SDK's [[WorkflowOptions]] */
  def fromJava(options: WorkflowOptions): ZWorkflowOptions =
    ZWorkflowOptions(
      workflowId = options.getWorkflowId,
      taskQueue = options.getTaskQueue,
      workflowIdReusePolicy = Option(options.getWorkflowIdReusePolicy),
      workflowRunTimeout = Option(options.getWorkflowRunTimeout),
      workflowExecutionTimeout = Option(options.getWorkflowExecutionTimeout),
      workflowTaskTimeout = Option(options.getWorkflowTaskTimeout),
      retryOptions = Option(options.getRetryOptions).map(ZRetryOptions.fromJava),
      memo = Option(options.getMemo).map(_.asScala.toMap).getOrElse(Map.empty),
      searchAttributes = Option(options.getTypedSearchAttributes).map(ZSearchAttributes.fromJava),
      contextPropagators = Option(options.getContextPropagators).toList.flatMap(_.asScala.toList),
      disableEagerExecution = Some(options.isDisableEagerExecution),
      javaOptionsCustomization = identity
    )
}
