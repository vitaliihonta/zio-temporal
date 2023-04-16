package zio.temporal.internal

import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.{BatchRequest, WorkflowStub}
import io.temporal.workflow.Functions
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
    args:         List[AnyRef]
  )(implicit ctg: ClassTag[R]
  ): CompletableFuture[R] = {
    start(stub, args)
    stub.getResultAsync(timeout.toNanos, TimeUnit.NANOSECONDS, ClassTagUtils.classOf[R])
  }

  def addToBatchRequest(f: () => Unit): BatchRequest => Unit = { (b: BatchRequest) =>
    addToBatchRequest(b, f)
  }

  def addToBatchRequest(b: BatchRequest, f: () => Unit): Unit =
    b.add(f: Functions.Proc)

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
