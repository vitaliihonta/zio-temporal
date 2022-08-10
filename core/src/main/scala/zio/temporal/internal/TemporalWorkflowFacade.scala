package zio.temporal.internal

import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.{BatchRequest, WorkflowClient, WorkflowStub}
import io.temporal.workflow.Functions

import java.util.concurrent.CompletableFuture
import scala.language.implicitConversions
import scala.reflect.ClassTag

object TemporalWorkflowFacade {
  import FunctionConverters.*

  def start(f: () => Unit): WorkflowExecution =
    WorkflowClient.start(f: Functions.Proc)

  def execute[R](f: () => R): CompletableFuture[R] =
    WorkflowClient.execute[R](f: Functions.Func[R])

  def addToBatchRequest(f: () => Unit): BatchRequest => Unit = { (b: BatchRequest) =>
    addToBatchRequest(b, f)
  }

  def addToBatchRequest(b: BatchRequest, f: () => Unit): Unit =
    b.add(f: Functions.Proc)

  def query[R](stub: WorkflowStub, name: String, args: List[AnyRef])(implicit ctg: ClassTag[R]): R =
    stub.query[R](name, ClassTagUtils.classOf[R], args: _*)

  object FunctionConverters {
    implicit def proc(f: () => Unit): Functions.Proc = new Functions.Proc {
      override def apply(): Unit = f()
    }
    implicit def func0[A, R](f: () => R): Functions.Func[R] = new Functions.Func[R] {
      override def apply(): R = f()
    }
  }
}
