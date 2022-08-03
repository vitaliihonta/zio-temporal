package zio.temporal.workflow

import zio.*
import io.temporal.client.WorkflowClient
import scala.compat.java8.OptionConverters._
import scala.quoted.*

trait ZWorkflowStubProxySyntax extends Any {
  def toJava: WorkflowClient

  /** Creates workflow untyped client stub for a known execution. Use it to send signals or queries to a running
    * workflow. Do not call methods annotated with @WorkflowMethod.
    *
    * @see
    *   [[ZWorkflowStub]]
    */
  inline def newWorkflowStubProxy[A: IsConcreteType](
    workflowId: String,
    runId:      Option[String] = None
  ): UIO[ZWorkflowStub.Proxy[A]] =
    ${ ZWorkflowStubProxySyntax.newWorkflowStubProxyImpl[A]('toJava, 'workflowId, 'runId) }
}

object ZWorkflowStubProxySyntax {
  // TODO: implement without cast?
  def newWorkflowStubProxyImpl[A: Type](
    toJava:     Expr[WorkflowClient],
    workflowId: Expr[String],
    runId:      Expr[Option[String]]
  )(using q:    Quotes
  ): Expr[UIO[ZWorkflowStub.Proxy[A]]] = {
    val result = '{
      ZIO.succeed {
        ZWorkflowStub.Proxy[A](
          new ZWorkflowStub($toJava.newUntypedWorkflowStub($workflowId, $runId.asJava, Option.empty[String].asJava))
        )
      }
    }
    println(result.show)
    result
  }
}
