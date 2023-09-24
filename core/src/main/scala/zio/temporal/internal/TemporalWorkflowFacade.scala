package zio.temporal.internal

import io.temporal.activity.{ActivityOptions, LocalActivityOptions}
import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.schedules.ScheduleActionStartWorkflow
import io.temporal.client.{WorkflowClient, WorkflowOptions, WorkflowStub}
import io.temporal.common.interceptors.Header
import io.temporal.workflow.{
  ActivityStub,
  ChildWorkflowOptions,
  ChildWorkflowStub,
  ContinueAsNewOptions,
  ExternalWorkflowStub,
  Functions,
  Promise,
  Workflow
}

import java.util.concurrent.{CompletableFuture, TimeUnit}
import scala.language.implicitConversions
import scala.reflect.ClassTag
import zio.{Duration, UIO, ZIO}
import zio.temporal.JavaTypeTag
import zio.temporal.activity.{ZActivityStub, ZActivityStubImpl}
import zio.temporal.schedules.ZScheduleAction
import zio.temporal.workflow.{ZChildWorkflowStub, ZChildWorkflowStubImpl, ZWorkflowStub, ZWorkflowStubImpl}

object TemporalWorkflowFacade {
  import FunctionConverters._

  def start(stub: WorkflowStub, args: List[Any]): WorkflowExecution =
    stub.start(args.asInstanceOf[List[AnyRef]]: _*)

  def startScheduleAction(
    stubbedClass:    Class[_],
    header:          Header,
    workflowOptions: WorkflowOptions,
    args:            List[Any]
  ): ZScheduleAction.StartWorkflow = {
    new ZScheduleAction.StartWorkflow(
      ScheduleActionStartWorkflow
        .newBuilder()
        .setWorkflowType(stubbedClass)
        .setHeader(header)
        .setOptions(workflowOptions)
        .setArguments(args.asInstanceOf[List[AnyRef]]: _*)
        .build()
    )
  }

  def execute[R](
    stub:                 WorkflowStub,
    args:                 List[Any]
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): CompletableFuture[R] = {
    start(stub, args)
    stub.getResultAsync(javaTypeTag.klass, javaTypeTag.genericType)
  }

  def executeWithTimeout[R](
    stub:                 WorkflowStub,
    timeout:              Duration,
    args:                 List[Any]
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): CompletableFuture[R] = {
    start(stub, args)
    stub.getResultAsync(timeout.toNanos, TimeUnit.NANOSECONDS, javaTypeTag.klass, javaTypeTag.genericType)
  }

  def executeChild[R](
    stub:                 ChildWorkflowStub,
    args:                 List[Any]
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): R = {
    stub.execute(javaTypeTag.klass, javaTypeTag.genericType, args.asInstanceOf[List[AnyRef]]: _*)
  }

  def executeChildAsync[R](
    stub:                 ChildWorkflowStub,
    args:                 List[Any]
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): Promise[R] = {
    stub.executeAsync(javaTypeTag.klass, javaTypeTag.genericType, args.asInstanceOf[List[AnyRef]]: _*)
  }

  def executeActivity[R](
    stub:                 ActivityStub,
    stubbedClass:         Class[_],
    methodName:           String,
    args:                 List[Any]
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): R = {
    stub.execute[R](
      ClassTagUtils.getActivityType(stubbedClass, methodName),
      javaTypeTag.klass,
      javaTypeTag.genericType,
      args.asInstanceOf[List[AnyRef]]: _*
    )
  }

  def executeActivityAsync[R](
    stub:                 ActivityStub,
    stubbedClass:         Class[_],
    methodName:           String,
    args:                 List[Any]
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): Promise[R] = {
    stub.executeAsync[R](
      ClassTagUtils.getActivityType(stubbedClass, methodName),
      javaTypeTag.klass,
      javaTypeTag.genericType,
      args.asInstanceOf[List[AnyRef]]: _*
    )
  }

  def signal(
    stub:       WorkflowStub,
    signalName: String,
    args:       List[Any]
  ): Unit = {
    stub.signal(signalName, args.asInstanceOf[List[AnyRef]]: _*)
  }

  def signal(
    stub:       ChildWorkflowStub,
    signalName: String,
    args:       List[Any]
  ): Unit = {
    stub.signal(signalName, args.asInstanceOf[List[AnyRef]]: _*)
  }

  def signal(
    stub:       ExternalWorkflowStub,
    signalName: String,
    args:       List[Any]
  ): Unit = {
    stub.signal(signalName, args.asInstanceOf[List[AnyRef]]: _*)
  }

  def signalWithStart(
    stub:       WorkflowStub,
    signalName: String,
    signalArgs: Array[Any],
    startArgs:  Array[Any]
  ): WorkflowExecution = {
    stub.signalWithStart(signalName, signalArgs.asInstanceOf[Array[AnyRef]], startArgs.asInstanceOf[Array[AnyRef]])
  }

  def query[R](
    stub:                 WorkflowStub,
    name:                 String,
    args:                 List[Any]
  )(implicit javaTypeTag: JavaTypeTag[R]
  ): R =
    stub.query[R](
      name,
      javaTypeTag.klass,
      javaTypeTag.genericType,
      args.asInstanceOf[List[AnyRef]]: _*
    )

  def createWorkflowStubTyped[A: ClassTag](client: WorkflowClient): WorkflowOptions => UIO[ZWorkflowStub.Of[A]] =
    options =>
      ZIO.succeed {
        ZWorkflowStub.Of[A](
          new ZWorkflowStubImpl(
            client.newUntypedWorkflowStub(
              ClassTagUtils.getWorkflowType[A],
              options
            ),
            ClassTagUtils.classOf[A]
          )
        )
      }

  def createWorkflowStubUntyped(
    workflowType: String,
    client:       WorkflowClient
  ): WorkflowOptions => UIO[ZWorkflowStub.Untyped] =
    options =>
      ZIO.succeed {
        new ZWorkflowStub.UntypedImpl(
          client.newUntypedWorkflowStub(workflowType, options)
        )
      }

  def continueAsNew[R](workflowType: String, options: ContinueAsNewOptions, args: List[Any]): R = {
    Workflow.continueAsNew(workflowType, options, args.asInstanceOf[List[AnyRef]]: _*)
    // continueAsNew never returns
    null.asInstanceOf[R]
  }

  def buildActivityStubTyped[A: ClassTag]: ActivityOptions => ZActivityStub.Of[A] =
    options =>
      ZActivityStub.Of[A](
        new ZActivityStubImpl(
          Workflow.newUntypedActivityStub(options),
          ClassTagUtils.classOf[A]
        )
      )

  def buildActivityStubUntyped: ActivityOptions => ZActivityStub.Untyped =
    options =>
      new ZActivityStub.UntypedImpl(
        Workflow.newUntypedActivityStub(options)
      )

  def buildLocalActivityStubTyped[A: ClassTag]: LocalActivityOptions => ZActivityStub.Of[A] =
    options =>
      ZActivityStub.Of[A](
        new ZActivityStubImpl(
          Workflow.newUntypedLocalActivityStub(options),
          ClassTagUtils.classOf[A]
        )
      )

  def buildLocalActivityStubUntyped: LocalActivityOptions => ZActivityStub.Untyped =
    options =>
      new ZActivityStub.UntypedImpl(
        Workflow.newUntypedLocalActivityStub(options)
      )

  def buildChildWorkflowStubTyped[A: ClassTag]: ChildWorkflowOptions => ZChildWorkflowStub.Of[A] =
    options =>
      ZChildWorkflowStub.Of(
        new ZChildWorkflowStubImpl(
          Workflow.newUntypedChildWorkflowStub(
            ClassTagUtils.getWorkflowType[A],
            options
          ),
          ClassTagUtils.classOf[A]
        )
      )

  def buildChildWorkflowStubUntyped(workflowType: String): ChildWorkflowOptions => ZChildWorkflowStub.Untyped =
    options =>
      new ZChildWorkflowStub.UntypedImpl(
        Workflow.newUntypedChildWorkflowStub(workflowType, options)
      )

  object FunctionConverters {
    implicit def proc(f: () => Unit): Functions.Proc = new Functions.Proc {
      override def apply(): Unit = f()
    }
    implicit def func0[R](f: () => R): Functions.Func[R] = new Functions.Func[R] {
      override def apply(): R = f()
    }
    implicit def func1[A, R](f: A => R): Functions.Func1[A, R] = new Functions.Func1[A, R] {
      override def apply(value: A): R = f(value)
    }
  }
}
