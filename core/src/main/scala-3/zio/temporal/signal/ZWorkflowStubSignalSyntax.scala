package zio.temporal.signal

import io.temporal.client.BatchRequest
import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internal.InvocationMacroUtils
import zio.temporal.internalApi
import zio.temporal.workflow.ZWorkflowClient

import scala.quoted.*

// TODO: implement
trait ZWorkflowStubSignalSyntax {
  inline def signal(inline f: Unit): TemporalIO[TemporalClientError, Unit] =
    ${ ZWorkflowStubSignalSyntax.signalImpl('f) }
}

// TODO: implement
trait ZWorkflowClientSignalWithStartSyntax extends Any {

  /** Creates builder for SignalWithStart operation.
    *
    * @param f
    *   signal method call
    * @return
    *   the builder
    */
  def signalWith(f: Unit): ZSignalBuilder =
    ???
}

// TODO: implement
final class ZSignalBuilder @internalApi() (
  val __zio_temporal_workflowClient: ZWorkflowClient,
  val __zio_temporal_addSignal:      BatchRequest => Unit) {

  /** Invokes SignalWithStart operation.
    *
    * @param f
    *   workflow method to start
    * @return
    *   workflowExecution of the started and signaled workflow.
    */
  def start[A](f: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    ???
}

object ZWorkflowStubSignalSyntax {
  def signalImpl(f: Expr[Unit])(using q: Quotes): Expr[TemporalIO[TemporalClientError, Unit]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    val invocation = macroUtils.getMethodInvocation(Expr.betaReduce(f).asTerm.underlying)
//    assertWorkflow(invocation.instance.tpe)
//    if (!(invocation.instance.tpe <:< typeOf[BaseCanSignal])) {
//      error(s".signal should be called only on ZWorkflowStub and etc.")
//    }

    val method = invocation.getMethod("Signal method should not be an extension method!")

    val signalName = macroUtils.getSignalName(method.symbol)

    val theMethod =
      invocation.instance.select(invocation.instance.symbol.methodMember("__zio_temporal_invokeSignal").head)
    val invokeTree =
      Apply(
        theMethod,
        List(
          Literal(StringConstant(signalName)),
          Expr.ofList(invocation.args.map(_.asExprOf[AnyRef])).asTerm
        )
      )
    val result = invokeTree.asExprOf[TemporalIO[TemporalClientError, Unit]]
    println(result.show)
    result
  }
}
