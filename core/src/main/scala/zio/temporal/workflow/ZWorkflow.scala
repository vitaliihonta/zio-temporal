package zio.temporal.workflow

import com.uber.m3.tally.Scope
import io.temporal.workflow.{CancellationScope, ContinueAsNewOptions, Workflow}
import org.slf4j.Logger
import zio.temporal.activity._
import zio.temporal.internal.{ClassTagUtils, TemporalWorkflowFacade, ZWorkflowVersionSpecific}
import zio.temporal.{
  JavaTypeTag,
  TypeIsSpecified,
  ZCurrentTimeMillis,
  ZRetryOptions,
  ZSearchAttribute,
  ZSearchAttributes,
  ZWorkflowExecution,
  ZWorkflowInfo
}
import zio.{Random => _, _}
import java.util.UUID
import scala.jdk.OptionConverters._
import scala.reflect.ClassTag
import scala.util.Random

object ZWorkflow extends ZWorkflowVersionSpecific {

  val DefaultVersion: Int = Workflow.DEFAULT_VERSION

  /** Retrieves workflow info
    *
    * @see
    *   [[io.temporal.workflow.WorkflowInfo]]
    * @return
    *   [[ZWorkflowInfo]]
    */
  def info: ZWorkflowInfo = new ZWorkflowInfo(Workflow.getInfo)

  /** Returns current workflow version
    *
    * @see
    *   [[Workflow.getVersion]]
    * @param changeId
    *   identifier of a particular change. All calls to getVersion that share a changeId are guaranteed to return the
    *   same version number. Use this to perform multiple coordinated changes that should be enabled together.
    * @param minSupported
    *   min version supported for the change
    * @param maxSupported
    *   max version supported for the change
    * @return
    *   version
    */
  def version(changeId: String, minSupported: Int, maxSupported: Int): Int =
    Workflow.getVersion(changeId, minSupported, maxSupported)

  /** Get logger to use inside workflow. Logs in replay mode are omitted unless
    * [[zio.temporal.worker.ZWorkerFactoryOptions.enableLoggingInReplay]] is set to 'true'.
    *
    * @param name
    *   name to appear in logging.
    * @return
    *   logger to use in workflow logic.
    */
  def getLogger(name: String): Logger = Workflow.getLogger(name)

  /** Get logger to use inside workflow. Logs in replay mode are omitted unless
    * [[zio.temporal.worker.ZWorkerFactoryOptions.enableLoggingInReplay]] is set to 'true'.
    *
    * @param clazz
    *   class name to appear in logging.
    * @return
    *   logger to use in workflow logic.
    */
  def getLogger(clazz: Class[_]): Logger = Workflow.getLogger(clazz)

  /** Suspends workflow execution until the given duration elapsed
    *
    * @param duration
    *   time to sleep
    * @see
    *   [[Workflow.sleep]]
    * @return
    *   unblocks when duration elapsed
    */
  def sleep(duration: Duration): Unit =
    Workflow.sleep(duration.asJava)

  /** Suspends workflow execution while the given predicate holds
    *
    * @param cond
    *   await condition
    * @see
    *   [[Workflow.await]]
    * @return
    *   unblocks when condition becomes false
    */
  def awaitWhile(cond: => Boolean): Unit =
    awaitUntil(!cond)

  /** Suspends workflow execution while either the given predicate holds or timeout elapsed
    *
    * @param cond
    *   await condition
    * @param timeout
    *   await timeout
    * @see
    *   [[Workflow.await]]
    * @return
    *   unblocks when condition becomes false or timeout elapsed. '''False''' if timed out
    */
  def awaitWhile(timeout: Duration)(cond: => Boolean): Boolean =
    awaitUntil(timeout)(!cond)

  /** Suspends workflow execution until the given predicate holds
    *
    * @param cond
    *   await condition
    * @see
    *   [[Workflow.await]]
    * @return
    *   unblocks when condition becomes true
    */
  def awaitUntil(cond: => Boolean): Unit =
    Workflow.await(() => cond)

  /** Suspends workflow execution until either the given predicate holds or timeout elapsed
    *
    * @param cond
    *   await condition
    * @param timeout
    *   await timeout
    * @see
    *   [[Workflow.await]]
    * @return
    *   unblocks when condition becomes true or timeout elapsed. '''False''' if timed out
    */
  def awaitUntil(timeout: Duration)(cond: => Boolean): Boolean =
    Workflow.await(timeout.asJava, () => cond)

  /** Create new timer. Note that Temporal service time resolution is in seconds. So all durations are rounded <b>up</b>
    * to the nearest second.
    *
    * @return
    *   `ZAsync` that becomes ready when at least specified number of seconds passes. It is failed with
    *   [[zio.temporal.failure.CanceledFailure]] if enclosing scope is canceled.
    */
  def newTimer(delay: Duration): ZAsync[Unit] =
    ZAsync.fromJava(Workflow.newTimer(delay)).unit

  /** Wraps a procedure in a CancellationScope. The procedure receives the wrapping CancellationScope as a parameter.
    * Useful when cancellation is requested from within the wrapped code. The following example cancels the sibling
    * activity on any failure.
    *
    * @see
    *   [[Workflow.newCancellationScope]]
    * @param thunk
    *   code to wrap in the cancellation scope
    * @return
    *   wrapped proc
    */
  def newCancellationScope[U](thunk: => U): ZCancellationScope = {
    val scope = Workflow.newCancellationScope(() => (thunk: Unit))
    new ZCancellationScope(scope)
  }

  /** Wraps a procedure in a CancellationScope. The procedure receives the wrapping CancellationScope as a parameter.
    * Useful when cancellation is requested from within the wrapped code. The following example cancels the sibling
    * activity on any failure.
    * @see
    *   [[Workflow.newCancellationScope]]
    * @param thunk
    *   code to wrap in the cancellation scope
    * @return
    *   wrapped proc
    */
  def newCancellationScope[U](thunk: ZCancellationScope => U): ZCancellationScope = {
    val scope =
      Workflow.newCancellationScope((scope: CancellationScope) => (thunk(new ZCancellationScope(scope)): Unit))
    new ZCancellationScope(scope)
  }

  /** Creates a CancellationScope that is not linked to a parent scope. [[ZCancellationScope.run()]] must be called to
    * execute the code the scope wraps. The detached scope is needed to execute cleanup code after a workflow is
    * canceled which cancels the root scope that wraps the @WorkflowMethod invocation. Here is an example usage:
    *
    * @see
    *   [[Workflow.newDetachedCancellationScope]]
    * @param thunk
    *   parameter to wrap in a cancellation scope.
    * @return
    *   wrapped parameter.
    */
  def newDetachedCancellationScope[U](thunk: => U): ZCancellationScope = {
    val scope = Workflow.newDetachedCancellationScope(() => thunk)
    new ZCancellationScope(scope)
  }

  /** Generated random [[UUID]]
    *
    * Should be used instead of [[UUID.randomUUID()]] to guarantee determinism
    * @see
    *   [[Workflow.randomUUID()]]
    * @return
    *   generated [[UUID]]
    */
  def randomUUID: UUID = Workflow.randomUUID()

  /** Replay safe random numbers generator. Seeded differently for each workflow instance. */
  def newRandom: Random =
    Workflow.newRandom()

  /** Executes the provided function once, records its result into the workflow history. The recorded result on history
    * will be returned without executing the provided function during replay. This guarantees the deterministic
    * requirement for workflow as the exact same result will be returned in replay. Common use case is to run some short
    * non-deterministic code in workflow, like getting random number. The only way to fail SideEffect is to panic which
    * causes workflow task failure. The workflow task after timeout is rescheduled and re-executed giving SideEffect
    * another chance to succeed.
    *
    * If function throws any exception it is not delivered to the workflow code. It is wrapped in [[Error]] causing
    * failure of the current workflow task.
    *
    * @tparam R
    *   side effect result type
    * @param f
    *   function that returns side effect value
    * @return
    *   value of the side effect
    * @see
    *   [[mutableSideEffect]]
    */
  def sideEffect[R](f: () => R)(implicit javaTypeTag: JavaTypeTag[R]): R =
    Workflow.sideEffect[R](javaTypeTag.klass, javaTypeTag.genericType, () => f())

  /** `mutableSideEffect` is similar to [[sideEffect]] in allowing calls of non-deterministic functions from workflow
    * code.
    *
    * <p>The difference between [[mutableSideEffect]] and [[sideEffect]] is that every new [[sideEffect]] call in
    * non-replay mode results in a new marker event recorded into the history. However, [[mutableSideEffect]] only
    * records a new marker if a value has changed. During the replay, `mutableSideEffect` will not execute the function
    * again, but it will return the exact same value as it was returning during the non-replay run.
    *
    * <p>One good use case of `mutableSideEffect` is to access a dynamically changing config without breaking
    * determinism. Even if called very frequently the config value is recorded only when it changes not causing any
    * performance degradation due to a large history size.
    *
    * <p>Caution: do not use `mutableSideEffect` function to modify any workflow state. Only use the mutableSideEffect's
    * return value.
    *
    * <p>If function throws any exception it is not delivered to the workflow code. It is wrapped in [[Error]] causing
    * failure of the current workflow task.
    *
    * @tparam R
    *   side effect result type
    * @param id
    *   unique identifier of this side effect
    * @param updated
    *   used to decide if a new value should be recorded. A func result is recorded only if call to updated with stored
    *   and a new value as arguments returns true. It is not called for the first value.
    * @param f
    *   function that produces a value. This function can contain non-deterministic code.
    * @see
    *   [[sideEffect]]
    */
  def mutableSideEffect[R](
    id:                   String,
    updated:              (R, R) => Boolean,
    f:                    () => R
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): R =
    Workflow.mutableSideEffect(id, javaTypeTag.klass, javaTypeTag.genericType, (a, b) => updated(a, b), () => f())

  /** Returns current timestamp
    *
    * Should be used instead of [[java.lang.System.currentTimeMillis()]] to guarantee determinism
    * @see
    *   [[Workflow.currentTimeMillis()]]
    * @return
    *   current time millis as [[ZCurrentTimeMillis]]
    */
  def currentTimeMillis: ZCurrentTimeMillis =
    new ZCurrentTimeMillis(Workflow.currentTimeMillis())

  /** Get immutable set of search attributes. To modify search attributes associated with this workflow use
    * [[upsertSearchAttributes]]
    *
    * @return
    *   immutable set of search attributes.
    */
  def typedSearchAttributes: ZSearchAttributes =
    ZSearchAttributes.fromJava(Workflow.getTypedSearchAttributes)

  /** Adds or updates workflow search attributes.
    *
    * @see
    *   [[Workflow.upsertTypedSearchAttributes]]
    * @param attrs
    *   map of String to [[ZSearchAttribute]] value that can be used to search in list APIs
    */
  def upsertSearchAttributes(attrs: Map[String, ZSearchAttribute]): Unit =
    Workflow.upsertTypedSearchAttributes(
      ZSearchAttribute.toJavaAttributeUpdates(attrs): _*
    )

  /** Creates a builder of client stub to activities that implement given interface.
    *
    * @tparam A
    *   activity interface
    * @return
    *   activity stub builder
    */
  def newActivityStub[A: ClassTag: IsActivity]: ZActivityStubBuilderInitial.Of[A] =
    new ZActivityStubBuilderInitial.Of[A](ZActivityStubBuilderInitial.buildTyped[A])

  /** Creates a builder of untyped client stub to activities
    *
    * @return
    *   untyped activity stub builder
    */
  def newUntypedActivityStub: ZActivityStubBuilderInitial.Untyped =
    new ZActivityStubBuilderInitial.Untyped(ZActivityStubBuilderInitial.buildUntyped)

  /** Creates a builder of client stub to local activities that implement given interface.
    *
    * @tparam A
    *   activity interface
    * @return
    *   local activity stub builder
    */
  def newLocalActivityStub[A: ClassTag: IsActivity]: ZLocalActivityStubBuilderInitial.Of[A] =
    new ZLocalActivityStubBuilderInitial.Of[A](ZLocalActivityStubBuilderInitial.buildTyped[A])

  /** Creates a builder of untyped client stub to local activities that implement given interface.
    *
    * @return
    *   local activity stub builder
    */
  def newUntypedLocalActivityStub: ZLocalActivityStubBuilderInitial.Untyped =
    new ZLocalActivityStubBuilderInitial.Untyped(ZLocalActivityStubBuilderInitial.buildUntyped)

  /** Creates a builder of client stub that can be used to start a child workflow that implements given interface.
    *
    * @tparam A
    *   workflow interface
    * @return
    *   child workflow stub builder
    */
  def newChildWorkflowStub[A: ClassTag: IsWorkflow]: ZChildWorkflowStubBuilder.Of[A] =
    new ZChildWorkflowStubBuilder.Of[A](ZChildWorkflowStubBuilder.buildTyped[A], identity)

  /** Creates a builder of untyped client stub that can be used to start a child workflow that implements given
    * interface.
    *
    * @return
    *   child workflow stub builder
    */
  def newUntypedChildWorkflowStub(workflowType: String): ZChildWorkflowStubBuilder.Untyped =
    new ZChildWorkflowStubBuilder.Untyped(ZChildWorkflowStubBuilder.buildUntyped(workflowType), identity)

  /** Creates client stub that can be used to signal or cancel an existing workflow
    *
    * @tparam A
    *   workflow interface
    * @param workflowId
    *   id of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newExternalWorkflowStub[A: ClassTag: IsWorkflow](
    workflowId: String
  ): ZExternalWorkflowStub.Of[A] =
    ZExternalWorkflowStub.Of(
      new ZExternalWorkflowStubImpl(
        Workflow.newUntypedExternalWorkflowStub(workflowId),
        ClassTagUtils.classOf[A]
      )
    )

  /** Creates client stub that can be used to signal or cancel an existing workflow
    *
    * @tparam A
    *   workflow interface
    * @param workflowExecution
    *   execution of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newExternalWorkflowStub[A: ClassTag: IsWorkflow](
    workflowExecution: ZWorkflowExecution
  ): ZExternalWorkflowStub.Of[A] =
    ZExternalWorkflowStub.Of(
      new ZExternalWorkflowStubImpl(
        Workflow.newUntypedExternalWorkflowStub(workflowExecution.toJava),
        ClassTagUtils.classOf[A]
      )
    )

  /** Creates untyped client stub that can be used to signal or cancel an existing workflow
    *
    * @param workflowId
    *   id of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newUntypedExternalWorkflowStub(
    workflowId: String
  ): ZExternalWorkflowStub.Untyped =
    new ZExternalWorkflowStub.UntypedImpl(
      Workflow.newUntypedExternalWorkflowStub(workflowId)
    )

  /** Creates untyped client stub that can be used to signal or cancel an existing workflow
    *
    * @param workflowExecution
    *   execution of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newUntypedExternalWorkflowStub(
    workflowExecution: ZWorkflowExecution
  ): ZExternalWorkflowStub.Untyped =
    new ZExternalWorkflowStub.UntypedImpl(
      Workflow.newUntypedExternalWorkflowStub(workflowExecution.toJava)
    )

  /** Creates a client stub that can be used to continue this workflow as a new run.
    *
    * @tparam A
    *   an interface type implemented by the next run of the workflow
    */
  def newContinueAsNewStub[A: ClassTag: IsWorkflow]: ZWorkflowContinueAsNewStubBuilder[A] =
    new ZWorkflowContinueAsNewStubBuilder[A](identity)

  /** Continues the current workflow execution as a new run possibly overriding the workflow type and options.
    *
    * @param workflowType
    *   workflow type override for the next run, can be null of no override is needed
    * @param options
    *   option overrides for the next run, can be null if no overrides are needed
    * @param args
    *   arguments of the next run.
    */
  def continueAsNew(workflowType: String, options: Option[ContinueAsNewOptions], args: Any*): Unit = {
    TemporalWorkflowFacade.continueAsNew[Any](workflowType, options.orNull, args.toList)
  }

  /** GetLastCompletionResult extract last completion result from previous run for this cron workflow. This is used in
    * combination with cron schedule. A workflow can be started with an optional cron schedule. If a cron workflow wants
    * to pass some data to next schedule, it can return any data and that data will become available when next run
    * starts.
    *
    * @tparam R
    *   type of the return data from last run
    * @return
    *   result of last run
    * @see
    *   io.temporal.client.WorkflowOptions.Builder#setCronSchedule(String)
    */
  def getLastCompletionResult[R: TypeIsSpecified: JavaTypeTag]: R =
    Workflow.getLastCompletionResult(JavaTypeTag[R].klass, JavaTypeTag[R].genericType)

  /** Extract the latest failure from a previous run of this workflow. If any previous run of this workflow has failed,
    * this function returns that failure. If no previous runs have failed, an empty optional is returned. The run you
    * are calling this from may have been created as a retry of the previous failed run or as a next cron invocation for
    * cron workflows.
    *
    * @return
    *   The last [[Exception]]that occurred in this workflow, if there has been one.
    */
  def getPreviousRunFailure: Option[Exception] =
    Workflow.getPreviousRunFailure.toScala

  /** Invokes function retrying in case of failures according to retry options. Synchronous variant.
    *
    * @param options
    *   retry options that specify retry policy
    * @param expiration
    *   stop retrying after this interval if provided
    * @param f
    *   function to invoke and retry
    * @return
    *   result of the function or the last failure.
    */
  def retry[R](options: ZRetryOptions, expiration: Option[Duration] = None)(f: => R): R =
    Workflow.retry(options.toJava, expiration.toJava, () => f)

  /** Get scope for reporting business metrics in workflow logic. This should be used instead of creating new metrics
    * scopes as it is able to dedupe metrics during replay.
    *
    * <p>The original metrics scope is set through [[ZWorkflowServiceStubsOptions.withMetricsScope]] when a worker
    * starts up.
    */
  def metricsScope: Scope =
    Workflow.getMetricsScope
}
