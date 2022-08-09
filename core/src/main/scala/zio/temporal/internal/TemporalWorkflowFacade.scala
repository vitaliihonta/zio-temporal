package zio.temporal.internal

import io.temporal.api.common.v1.WorkflowExecution
import io.temporal.client.WorkflowClient
import io.temporal.workflow.Functions

import java.util.concurrent.CompletableFuture
import scala.language.implicitConversions

object TemporalWorkflowFacade {
  import FunctionConverters.*

  def start(f: () => Unit): WorkflowExecution =
    WorkflowClient.start(f: Functions.Proc)

  def execute[R](f: () => R): CompletableFuture[R] =
    WorkflowClient.execute[R](f: Functions.Func[R])

  object FunctionConverters {
    implicit def proc(f: () => Unit): Functions.Proc = new Functions.Proc {
      override def apply(): Unit = f()
    }
    implicit def func0[A, R](f: () => R): Functions.Func[R] = new Functions.Func[R] {
      override def apply(): R = f()
    }
  }
}
