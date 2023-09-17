package zio.temporal.worker

import io.temporal.worker.WorkflowImplementationOptions
import zio.temporal.activity.{ZActivityType, ZActivityOptions, ZLocalActivityOptions}
import scala.jdk.CollectionConverters._

final case class ZWorkflowImplementationOptions private[zio] (
  failWorkflowExceptionTypes:  List[Class[_ <: Throwable]],
  activityOptions:             Map[String, ZActivityOptions],
  defaultActivityOptions:      Option[ZActivityOptions],
  localActivityOptions:        Map[String, ZLocalActivityOptions],
  defaultLocalActivityOptions: Option[ZLocalActivityOptions],
  private val javaOptionsCustomization: WorkflowImplementationOptions.Builder => WorkflowImplementationOptions.Builder) {

  /** Optional: Sets how workflow worker deals with exceptions thrown from the workflow code which include
    * non-deterministic history events (presumably arising from non-deterministic workflow definitions or non-backward
    * compatible workflow definition changes).
    *
    * <p>The default behavior is to fail workflow on [[io.temporal.failure.TemporalFailure]] or any of its subclasses.
    * Any other exceptions thrown from the workflow code are treated as bugs that can be fixed by a new deployment. So
    * workflow is not failed, but it stuck in a retry loop trying to execute the code that led to the unexpected
    * exception.
    *
    * <p>This option allows to specify specific exception types which should lead to workflow failure instead of
    * blockage. Any exception that extends the configured type considered matched. For example to fail workflow on any
    * exception pass [[Throwable]] class to this method.
    */
  def withFailWorkflowExceptionTypes(values: Class[_ <: Throwable]*): ZWorkflowImplementationOptions =
    withFailWorkflowExceptionTypes(values.toList)

  /** Optional: Sets how workflow worker deals with exceptions thrown from the workflow code which include
    * non-deterministic history events (presumably arising from non-deterministic workflow definitions or non-backward
    * compatible workflow definition changes).
    *
    * <p>The default behavior is to fail workflow on [[io.temporal.failure.TemporalFailure]] or any of its subclasses.
    * Any other exceptions thrown from the workflow code are treated as bugs that can be fixed by a new deployment. So
    * workflow is not failed, but it stuck in a retry loop trying to execute the code that led to the unexpected
    * exception.
    *
    * <p>This option allows to specify specific exception types which should lead to workflow failure instead of
    * blockage. Any exception that extends the configured type considered matched. For example to fail workflow on any
    * exception pass [[Throwable]] class to this method.
    */
  def withFailWorkflowExceptionTypes(values: List[Class[_ <: Throwable]]): ZWorkflowImplementationOptions =
    copy(failWorkflowExceptionTypes = values)

  /** Set individual activity options per activityType. Will be merged with the map from
    * [[zio.temporal.workflow.ZWorkflow.newActivityStub]] which has the highest precedence.
    *
    * @param values
    *   map from activityType to ActivityOptions
    */
  def withActivityOptions(values: (ZActivityType, ZActivityOptions)*): ZWorkflowImplementationOptions =
    withActivityOptions(values.toMap)

  /** Set individual activity options per activityType. Will be merged with the map from
    * [[zio.temporal.workflow.ZWorkflow.newActivityStub]] which has the highest precedence.
    *
    * @param values
    *   map from activityType to ActivityOptions
    */
  def withActivityOptions(values: Map[ZActivityType, ZActivityOptions]): ZWorkflowImplementationOptions =
    copy(activityOptions = values.map { case (at, options) => at.activityType -> options }.toMap)

  /** These activity options have the lowest precedence across all activity options. Will be overwritten entirely by
    * [[zio.temporal.workflow.ZWorkflow.newActivityStub]] and then by the individual activity options if any are set
    * through [[withActivityOptions]]
    *
    * @param value
    *   ZActivityOptions for all activities in the workflow.
    */
  def withDefaultActivityOptions(value: ZActivityOptions): ZWorkflowImplementationOptions =
    copy(defaultActivityOptions = Some(value))

  /** Set individual local activity options per activityType. Will be merged with the map from
    * [[zio.temporal.workflow.ZWorkflow.newLocalActivityStub]] which has the highest precedence.
    *
    * @param values
    *   map from activityType to ActivityOptions
    */
  def withLocalActivityOptions(values: (ZActivityType, ZLocalActivityOptions)*): ZWorkflowImplementationOptions =
    withLocalActivityOptions(values.toMap)

  /** Set individual local activity options per activityType. Will be merged with the map from
    * [[zio.temporal.workflow.ZWorkflow.newLocalActivityStub]] which has the highest precedence.
    *
    * @param values
    *   map from activityType to ActivityOptions
    */
  def withLocalActivityOptions(values: Map[ZActivityType, ZLocalActivityOptions]): ZWorkflowImplementationOptions =
    copy(localActivityOptions = values.map { case (at, options) => at.activityType -> options }.toMap)

  /** These local activity options have the lowest precedence across all local activity options. Will be overwritten
    * entirely by [[zio.temporal.workflow.ZWorkflow.newLocalActivityStub]] and then by the individual local activity
    * options if any are set through [[withLocalActivityOptions]]
    *
    * @param value
    *   ActivityOptions for all activities in the workflow.
    */
  def withDefaultLocalActivityOptions(value: ZLocalActivityOptions): ZWorkflowImplementationOptions =
    copy(defaultLocalActivityOptions = Some(value))

  /** Allows to specify options directly on the java SDK's [[WorkflowImplementationOptions]]. Use it in case an
    * appropriate `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkflowImplementationOptions.Builder => WorkflowImplementationOptions.Builder
  ): ZWorkflowImplementationOptions = copy(javaOptionsCustomization = f)

  /** Converts workflow implementation options to Java SDK's [[WorkflowImplementationOptions]]
    */
  def toJava: WorkflowImplementationOptions = {
    val builder = WorkflowImplementationOptions.newBuilder()

    builder.setFailWorkflowExceptionTypes(failWorkflowExceptionTypes: _*)
    builder.setActivityOptions(activityOptions.map { case (k, v) => k -> v.toJava }.asJava)
    defaultActivityOptions.foreach(o => builder.setDefaultActivityOptions(o.toJava))
    builder.setLocalActivityOptions(localActivityOptions.map { case (k, v) => k -> v.toJava }.asJava)
    defaultLocalActivityOptions.foreach(o => builder.setDefaultLocalActivityOptions(o.toJava))

    javaOptionsCustomization(builder).build()
  }

  override def toString: String = {
    s"ZWorkflowImplementationOptions(" +
      s"failWorkflowExceptionTypes=$failWorkflowExceptionTypes" +
      s", activityOptions=$activityOptions" +
      s", defaultActivityOptions=$defaultActivityOptions" +
      s", localActivityOptions=$localActivityOptions" +
      s", defaultLocalActivityOptions=$defaultLocalActivityOptions" +
      s")"
  }
}

object ZWorkflowImplementationOptions {
  val default: ZWorkflowImplementationOptions = {
    ZWorkflowImplementationOptions(
      failWorkflowExceptionTypes = Nil,
      activityOptions = Map.empty,
      defaultActivityOptions = None,
      localActivityOptions = Map.empty,
      defaultLocalActivityOptions = None,
      javaOptionsCustomization = identity
    )
  }
}
