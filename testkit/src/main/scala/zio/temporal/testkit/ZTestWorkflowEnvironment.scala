package zio.temporal.testkit

import io.temporal.testing.TestWorkflowEnvironment
import zio._
import zio.temporal.ZAwaitTerminationOptions
import zio.temporal.activity.ZActivityRunOptions
import zio.temporal.ZCurrentTimeMillis
import zio.temporal.internal.TemporalWorkflowFacade
import zio.temporal.worker.ZWorker
import zio.temporal.worker.ZWorkerOptions
import zio.temporal.workflow._
import java.util.concurrent.TimeUnit
import scala.reflect.ClassTag

/** TestWorkflowEnvironment provides workflow unit testing capabilities.
  *
  * <p>Testing the workflow code is hard as it might be potentially very long running. The included in-memory
  * implementation of the Temporal service supports <b>an automatic time skipping</b>. Anytime a workflow under the test
  * as well as the unit test code are waiting on a timer (or sleep) the internal service time is automatically advanced
  * to the nearest time that unblocks one of the waiting threads. This way a workflow that runs in production for months
  * is unit tested in milliseconds. Here is an example of a test that executes in a few milliseconds instead of over two
  * hours that are needed for the workflow to complete:
  *
  * @see
  *   [[TestWorkflowEnvironment]]
  */
class ZTestWorkflowEnvironment[+R] private[zio] (val toJava: TestWorkflowEnvironment, runtime: zio.Runtime[R]) {

  def namespace: String =
    toJava.getNamespace

  /** Creates a new Worker instance that is connected to the in-memory test Temporal service.
    *
    * @param taskQueue
    *   task queue to poll.
    */
  def newWorker(taskQueue: String, options: ZWorkerOptions = ZWorkerOptions.default): UIO[ZWorker] =
    ZIO.succeedBlocking(
      new ZWorker(toJava.newWorker(taskQueue, options.toJava))
    )

  /** Creates a WorkflowClient that is connected to the in-memory test Temporal service. */
  lazy val workflowClient: ZWorkflowClient = new ZWorkflowClient(toJava.getWorkflowClient)

  /** Returns the in-memory test Temporal service that is owned by this. */
  lazy val workflowServiceStubs: ZWorkflowServiceStubs = new ZWorkflowServiceStubs(toJava.getWorkflowServiceStubs)

  implicit lazy val activityRunOptions: ZActivityRunOptions[R] =
    new ZActivityRunOptions[R](runtime, Some(workflowClient.toJava.newActivityCompletionClient()))

  /** Setup test environment with a guaranteed finalization.
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  def setup(options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault): URIO[Scope, Unit] =
    for {
      _ <- start
      _ <- ZIO.addFinalizer(close)
    } yield ()

  /** Start all workers created by this test environment. */
  def start: UIO[Unit] =
    ZIO.succeedBlocking(toJava.start())

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * executed.
    *
    * @see
    *   [[TestWorkflowEnvironment#shutdown]]
    */
  def shutdown: UIO[Unit] =
    ZIO.succeedBlocking(toJava.shutdown())

  /** Initiates an orderly shutdown in which polls are stopped and already received workflow and activity tasks are
    * attempted to be stopped. This implementation cancels tasks via Thread.interrupt(), so any task that fails to
    * respond to interrupts may never terminate.
    *
    * @see
    *   [[TestWorkflowEnvironment#shutdownNow]]
    */
  def shutdownNow: UIO[Unit] =
    ZIO.succeedBlocking(toJava.shutdownNow())

  /** @see
    *   [[TestWorkflowEnvironment#close]]
    */
  def close: UIO[Unit] =
    ZIO.succeedBlocking(toJava.close())

  /** Blocks until all tasks have completed execution after a shutdown request, or the timeout occurs, or the current
    * thread is interrupted, whichever happens first.
    *
    * @param options
    *   await termination options
    */
  def awaitTermination(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault
  ): UIO[Unit] =
    ZIO
      .succeedBlocking {
        toJava.awaitTermination(options.pollTimeout.toNanos, TimeUnit.NANOSECONDS)
      }
      .repeat(Schedule.recurUntil((_: Unit) => true) && Schedule.fixed(options.pollDelay))
      .unit

  /** This time might not be equal to [[java.lang.System.currentTimeMillis()]] due to time skipping.
    *
    * @return
    *   the current in-memory test Temporal service time in milliseconds or [[java.lang.System.currentTimeMillis()]] if
    *   an external service without time skipping support is used
    */
  def currentTimeMillis: UIO[ZCurrentTimeMillis] =
    ZIO.succeed(new ZCurrentTimeMillis(toJava.currentTimeMillis()))

  /** Wait until internal test Temporal service time passes the specified duration. This call also indicates that
    * workflow time might jump forward (if none of the activities are running) up to the specified duration.
    *
    * <p>This method falls back to [[Thread.sleep]] if an external service without time skipping support is used
    */
  def sleep(duration: Duration): UIO[Unit] =
    ZIO.succeedBlocking(toJava.sleep(duration))

  /** Currently prints histories of all workflow instances stored in the service. This is useful information to print in
    * the case of a unit test failure.
    *
    * @return
    *   the diagnostic data about the internal service state.
    */
  def getDiagnostics: UIO[String] =
    ZIO.succeed(toJava.getDiagnostics)
}

object ZTestWorkflowEnvironment {

  /** Setup test environment with a guaranteed finalization.
    *
    * @param options
    *   await options with polling interval and poll delay
    */
  def setup(
    options: ZAwaitTerminationOptions = ZAwaitTerminationOptions.testDefault
  ): URIO[ZTestWorkflowEnvironment[Any] with Scope, Unit] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](_.setup(options))

  /** Creates a new Worker instance that is connected to the in-memory test Temporal service.
    *
    * @param taskQueue
    *   task queue to poll.
    */
  def newWorker(
    taskQueue: String,
    options:   ZWorkerOptions = ZWorkerOptions.default
  ): URIO[ZTestWorkflowEnvironment[Any], ZWorker] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](_.newWorker(taskQueue, options))

  /** Access a WorkflowClient that is connected to the in-memory test Temporal service. */
  def workflowClientWithZIO[R: Tag, E, A](
    f: ZWorkflowClient => ZIO[R, E, A]
  ): ZIO[ZTestWorkflowEnvironment[Any] with R, E, A] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](testEnv => f(testEnv.workflowClient))

  /** Returns the in-memory test Temporal service that is owned by this. */
  def workflowServiceStubs: URIO[ZTestWorkflowEnvironment[Any], ZWorkflowServiceStubs] =
    ZIO.serviceWith[ZTestWorkflowEnvironment[Any]](_.workflowServiceStubs)

  def activityRunOptions[R: Tag]: URIO[ZTestWorkflowEnvironment[R], ZActivityRunOptions[R]] =
    ZIO.serviceWith[ZTestWorkflowEnvironment[R]](_.activityRunOptions)

  /** Access activity options */
  def activityRunOptionsWithZIO[R]: ActivityOptionsWithZIOPartiallyApplied[R] =
    new ActivityOptionsWithZIOPartiallyApplied[R]

  final class ActivityOptionsWithZIOPartiallyApplied[R](private val `dummy`: Boolean = true) extends AnyVal {
    def apply[R2 <: ZTestWorkflowEnvironment[R], E, A](
      f:            ZActivityRunOptions[R] => ZIO[R2, E, A]
    )(implicit tag: Tag[R]
    ): ZIO[R2, E, A] =
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[R]](testEnv => f(testEnv.activityRunOptions))
  }

  /** Creates new typed workflow stub builder
    *
    * @tparam A
    *   workflow interface
    * @return
    *   builder instance
    */
  @deprecated("Use newWorkflowStub accepting ZWorkerOptions", since = "0.6.0")
  def newWorkflowStub[
    A: ClassTag: IsWorkflow
  ]: ZWorkflowStubBuilderTaskQueueDsl[URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Of[A]]] =
    new ZWorkflowStubBuilderTaskQueueDsl[URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Of[A]]](options =>
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        TemporalWorkflowFacade.createWorkflowStubTyped[A](testEnv.workflowClient.toJava).apply(options)
      }
    )

  /** Creates workflow client stub that can be used to start a single workflow execution. The first call must be to a
    * method annotated with @[[zio.temporal.workflowMethod]]. After workflow is started it can be also used to send
    * signals or queries to it. one.
    *
    * @tparam A
    *   workflow interface
    * @param options
    *   options used to start a workflow through returned stub
    * @return
    *   builder instance
    */
  def newWorkflowStub[A: ClassTag: IsWorkflow](
    options: ZWorkflowOptions
  ): URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Of[A]] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
      TemporalWorkflowFacade.createWorkflowStubTyped[A](testEnv.workflowClient.toJava).apply(options.toJava)
    }

  /** Creates new untyped type workflow stub builder
    *
    * @param workflowType
    *   name of the workflow type
    * @return
    *   builder instance
    */
  @deprecated("Use newUntypedWorkflowStub accepting ZWorkerOptions", since = "0.6.0")
  def newUntypedWorkflowStub(
    workflowType: String
  ): ZWorkflowStubBuilderTaskQueueDsl[URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Untyped]] =
    new ZWorkflowStubBuilderTaskQueueDsl[URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Untyped]](options =>
      ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
        TemporalWorkflowFacade.createWorkflowStubUntyped(workflowType, testEnv.workflowClient.toJava).apply(options)
      }
    )

  /** Creates workflow untyped client stub that can be used to start a single workflow execution. After workflow is
    * started it can be also used to send signals or queries to it.
    *
    * @param workflowType
    *   name of the workflow type
    * @param options
    *   options used to start a workflow through returned stub
    * @return
    *   builder instance
    */
  def newUntypedWorkflowStub(
    workflowType: String,
    options:      ZWorkflowOptions
  ): URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Untyped] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]] { testEnv =>
      TemporalWorkflowFacade
        .createWorkflowStubUntyped(workflowType, testEnv.workflowClient.toJava)
        .apply(options.toJava)
    }

  /** Creates workflow client stub for a known execution.
    *
    * @tparam A
    *   interface that given workflow implements.
    * @param workflowId
    *   Workflow id.
    * @param runId
    *   Run id of the workflow execution.
    * @return
    *   Stub that implements workflowInterface and can be used to signal or query it.
    */
  def newWorkflowStub[A: ClassTag: IsWorkflow](
    workflowId: String,
    runId:      Option[String] = None
  ): URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Of[A]] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](_.workflowClient.newWorkflowStub[A](workflowId, runId))

  /** Creates workflow untyped client stub for a known execution.
    *
    * @param workflowId
    *   workflow id and optional run id for execution
    * @param runId
    *   runId of the workflow execution. If not provided the last workflow with the given workflowId is assumed.
    * @return
    *   Stub that can be used to start workflow and later to signal or query it.
    */
  def newUntypedWorkflowStub(
    workflowId: String,
    runId:      Option[String]
  ): URIO[ZTestWorkflowEnvironment[Any], ZWorkflowStub.Untyped] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](_.workflowClient.newUntypedWorkflowStub(workflowId, runId))

  /** This time might not be equal to [[java.lang.System.currentTimeMillis()]] due to time skipping.
    *
    * @return
    *   the current in-memory test Temporal service time in milliseconds or [[java.lang.System.currentTimeMillis()]] if
    *   an external service without time skipping support is used
    */
  def currentTimeMillis: URIO[ZTestWorkflowEnvironment[Any], ZCurrentTimeMillis] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](_.currentTimeMillis)

  /** Wait until internal test Temporal service time passes the specified duration. This call also indicates that
    * workflow time might jump forward (if none of the activities are running) up to the specified duration.
    *
    * <p>This method falls back to [[Thread.sleep]] if an external service without time skipping support is used
    */
  def sleep(duration: Duration): URIO[ZTestWorkflowEnvironment[Any], Unit] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](_.sleep(duration))

  /** Currently prints histories of all workflow instances stored in the service. This is useful information to print in
    * the case of a unit test failure.
    *
    * @return
    *   the diagnostic data about the internal service state.
    */
  def getDiagnostics: URIO[ZTestWorkflowEnvironment[Any], String] =
    ZIO.serviceWithZIO[ZTestWorkflowEnvironment[Any]](_.getDiagnostics)

  /** Creates a new instance of [[ZTestWorkflowEnvironment]]
    *
    * @see
    *   [[TestWorkflowEnvironment.newInstance]]
    * @return
    *   managed instance of test environment
    */
  def make[R: Tag]: URLayer[R with ZTestEnvironmentOptions, ZTestWorkflowEnvironment[R]] =
    ZLayer.scoped[R with ZTestEnvironmentOptions] {
      ZIO.runtime[R with ZTestEnvironmentOptions].flatMap { runtime =>
        ZIO.succeedBlocking(
          new ZTestWorkflowEnvironment[R](
            TestWorkflowEnvironment.newInstance(runtime.environment.get[ZTestEnvironmentOptions].toJava),
            runtime
          )
        )
      }
    }

  /** Creates a new instance of [[ZTestWorkflowEnvironment]] with default options
    * @return
    *   managed instance of test environment
    */
  def makeDefault[R: Tag]: URLayer[R, ZTestWorkflowEnvironment[R]] = {
    ZLayer.makeSome[R, ZTestWorkflowEnvironment[R]](
      ZTestEnvironmentOptions.default,
      make[R]
    )
  }
}
