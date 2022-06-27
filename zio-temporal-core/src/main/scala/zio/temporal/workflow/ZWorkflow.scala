package zio.temporal.workflow

import io.temporal.workflow.CancellationScope
import io.temporal.workflow.Workflow
import zio.temporal.activity.ZActivityStubBuilderInitial
import zio.temporal.activity.ZLocalActivityStubBuilderInitial
import zio.temporal.internal.ClassTagUtils
import zio.temporal.ZCurrentTimeMillis
import zio.temporal.ZSearchAttribute
import zio.temporal.ZWorkflowExecution
import zio.temporal.ZWorkflowInfo

import java.util.UUID
import scala.compat.java8.DurationConverters._
import scala.concurrent.duration.FiniteDuration
import scala.jdk.CollectionConverters._
import scala.reflect.ClassTag

object ZWorkflow {

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

  /** Suspends workflow execution until the given duration elapsed
    *
    * @param duration
    *   time to sleep
    * @see
    *   [[Workflow.sleep]]
    * @return
    *   unblocks when duration elapsed
    */
  def sleep(duration: FiniteDuration): Unit =
    Workflow.sleep(duration.toJava)

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
    *   unblocks when condition becomes false or timeout elapsed
    */
  def awaitWhile(timeout: FiniteDuration)(cond: => Boolean): Boolean =
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
    *   unblocks when condition becomes true or timeout elapsed
    */
  def awaitUntil(timeout: FiniteDuration)(cond: => Boolean): Boolean =
    Workflow.await(timeout.toJava, () => cond)

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
    val scope = Workflow.newCancellationScope((scope: CancellationScope) => thunk(new ZCancellationScope(scope)))
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
    * Should be used instead of [[System.currentTimeMillis()]] to guarantee determinism
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
  def newActivityStub[A: ClassTag] =
    new ZActivityStubBuilderInitial[A](ClassTagUtils.classTagOf[A])

  /** Creates a builder of client stub to local activities that implement given interface.
    *
    * @tparam A
    *   activity interface
    * @return
    *   local activity stub builder
    */
  def newLocalActivityStub[A: ClassTag]: ZLocalActivityStubBuilderInitial[A] =
    new ZLocalActivityStubBuilderInitial[A](ClassTagUtils.classTagOf[A])

  /** Creates a builder of client stub that can be used to start a child workflow that implements given interface.
    *
    * @tparam A
    *   workflow interface
    * @return
    *   child workflow stub builder
    */
  def newChildWorkflowStub[A: ClassTag]: ZChildWorkflowStubBuilder[A] =
    new ZChildWorkflowStubBuilder[A](identity)

  /** Creates client stub that can be used to communicate to an existing workflow execution.
    *
    * @tparam A
    *   workflow interface
    * @param workflowId
    *   id of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newExternalWorkflowStub[A: ClassTag](workflowId: String): ZExternalWorkflowStub.Of[A] =
    ZExternalWorkflowStub.Of(
      Workflow.newExternalWorkflowStub[A](ClassTagUtils.classOf[A], workflowId)
    )

  /** Creates client stub that can be used to communicate to an existing workflow execution.
    *
    * @tparam A
    *   workflow interface
    * @param workflowExecution
    *   execution of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newExternalWorkflowStub[A: ClassTag](workflowExecution: ZWorkflowExecution): ZExternalWorkflowStub.Of[A] =
    ZExternalWorkflowStub.Of(
      Workflow.newExternalWorkflowStub[A](ClassTagUtils.classOf[A], workflowExecution.toJava)
    )

  /** Creates untyped client stub that can be used to communicate to an existing workflow execution.
    *
    * @param workflowId
    *   id of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newUntypedExternalWorkflowStub(workflowId: String): ZExternalWorkflowStub =
    new ZExternalWorkflowStub(Workflow.newUntypedExternalWorkflowStub(workflowId))

  /** Creates untyped client stub that can be used to communicate to an existing workflow execution.
    *
    * @param workflowExecution
    *   execution of the workflow to communicate with.
    * @return
    *   external workflow stub
    */
  def newUntypedExternalWorkflowStub(workflowExecution: ZWorkflowExecution): ZExternalWorkflowStub =
    new ZExternalWorkflowStub(Workflow.newUntypedExternalWorkflowStub(workflowExecution.toJava))
}
