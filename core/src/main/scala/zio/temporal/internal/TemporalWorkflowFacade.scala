package zio.temporal.internal

import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.{BatchRequest, WorkflowStub}
import io.temporal.workflow.{ActivityStub, ChildWorkflowStub, ExternalWorkflowStub, Functions, Promise}

import java.util.concurrent.{CompletableFuture, TimeUnit}
import scala.language.implicitConversions
import scala.reflect.ClassTag
import zio.Duration

object TemporalWorkflowFacade {
  import FunctionConverters.*

  def start(stub: WorkflowStub, args: List[Any]): WorkflowExecution =
    stub.start(args.asInstanceOf[List[AnyRef]]: _*)

  def execute[R](stub: WorkflowStub, args: List[Any])(implicit ctg: ClassTag[R]): CompletableFuture[R] = {
    start(stub, args)
    stub.getResultAsync(ClassTagUtils.classOf[R])
  }

  def executeWithTimeout[R](
    stub:         WorkflowStub,
    timeout:      Duration,
    args:         List[Any]
  )(implicit ctg: ClassTag[R]
  ): CompletableFuture[R] = {
    start(stub, args)
    stub.getResultAsync(timeout.toNanos, TimeUnit.NANOSECONDS, ClassTagUtils.classOf[R])
  }

  def executeChild[R](stub: ChildWorkflowStub, args: List[Any])(implicit ctg: ClassTag[R]): R = {
    stub.execute(ClassTagUtils.classOf[R], args.asInstanceOf[List[AnyRef]]: _*)
  }

  def executeChildAsync[R](stub: ChildWorkflowStub, args: List[Any])(implicit ctg: ClassTag[R]): Promise[R] = {
    stub.executeAsync(ClassTagUtils.classOf[R], args.asInstanceOf[List[AnyRef]]: _*)
  }

  def executeActivity[R](stub: ActivityStub, activityName: String, args: List[Any])(implicit ctg: ClassTag[R]): R = {
    stub.execute[R](activityName, ClassTagUtils.classOf[R], args.asInstanceOf[List[AnyRef]]: _*)
  }

  def executeActivityAsync[R](
    stub:         ActivityStub,
    activityName: String,
    args:         List[Any]
  )(implicit ctg: ClassTag[R]
  ): Promise[R] = {
    stub.executeAsync[R](activityName, ClassTagUtils.classOf[R], args.asInstanceOf[List[AnyRef]]: _*)
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

  def query[R](stub: WorkflowStub, name: String, args: List[Any])(implicit ctg: ClassTag[R]): R =
    stub.query[R](name, ClassTagUtils.classOf[R], args.asInstanceOf[List[AnyRef]]: _*)

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
