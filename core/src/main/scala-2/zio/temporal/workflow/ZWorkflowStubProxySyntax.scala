package zio.temporal.workflow

import zio.*
import io.temporal.client.WorkflowClient
import scala.compat.java8.OptionConverters._

trait ZWorkflowStubProxySyntax extends Any {
  def toJava: WorkflowClient

  /** Creates workflow untyped client stub for a known execution. Use it to send signals or queries to a running
    * workflow. Do not call methods annotated with @WorkflowMethod.
    *
    * @see
    *   [[ZWorkflowStub]]
    */
  def newWorkflowStubProxy[A: IsConcreteType](
    workflowId: String,
    runId:      Option[String] = None
  ): UIO[ZWorkflowStub.Proxy[A]] =
    ZIO.succeed {
      ZWorkflowStub.Proxy[A](
        new ZWorkflowStub(toJava.newUntypedWorkflowStub(workflowId, runId.asJava, Option.empty[String].asJava))
      )
    }
}
