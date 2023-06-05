package zio.temporal.workflow

import io.temporal.workflow.{CancellationScope, ContinueAsNewOptions, Workflow}
import org.slf4j.Logger
import zio.temporal.activity._
import zio.temporal.internal.{ClassTagUtils, TemporalWorkflowFacade, ZWorkflowVersionSpecific}
import zio.temporal.{
  JavaTypeTag,
  TypeIsSpecified,
  ZCurrentTimeMillis,
  ZSearchAttribute,
  ZWorkflowExecution,
  ZWorkflowInfo
}
import zio._
import java.util.UUID
import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._
import scala.reflect.ClassTag

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

  /** Returns current timestamp
    *
    * Should be used instead of [[java.lang.System.currentTimeMillis()]] to guarantee determinism
    * @see
    *   [[Workflow.currentTimeMillis()]]
    * @return
    *   current time millis as [[ZCurrentTimeMillis]]
    */
  def currentTimeMillis: ZCurrentTimeMillis = new ZCurrentTimeMillis(Workflow.currentTimeMillis())

  /** Adds or updates workflow search attributes.
    *
    * @see
    *   [[Workflow.upsertSearchAttributes]]
    * @param attrs
    *   map of String to [[ZSearchAttribute]] value that can be used to search in list APIs
    */
  def upsertSearchAttributes(attrs: Map[String, ZSearchAttribute]): Unit =
    Workflow.upsertSearchAttributes(
      attrs.map { case (k, v) => (k, v.attributeValue.asInstanceOf[AnyRef]) }.asJava
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
}
