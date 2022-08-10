package zio.temporal.signal

import io.temporal.client.BatchRequest
import zio.temporal.TemporalClientError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internalApi
import zio.temporal.workflow.ZWorkflowClient
import scala.quoted.*
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}

trait ZWorkflowStubSignalSyntax {
  inline def signal(inline f: Unit): TemporalIO[TemporalClientError, Unit] =
    ${ ZWorkflowStubSignalSyntax.signalImpl('f) }
}

trait ZWorkflowClientSignalWithStartSyntax { self: ZWorkflowClient =>

  /** Creates builder for SignalWithStart operation.
    *
    * @param f
    *   signal method call
    * @return
    *   the builder
    */
  inline def signalWith(inline f: Unit): ZSignalBuilder =
    ${ ZWorkflowStubSignalSyntax.signalWithStartBuilderImpl('self, 'f) }
}

final class ZSignalBuilder @internalApi() (
  val __zio_temporal_workflowClient: ZWorkflowClient,
  val __zio_temporal_addSignal:      BatchRequest => Unit) { self =>

  /** Invokes SignalWithStart operation.
    *
    * @param f
    *   workflow method to start
    * @return
    *   workflowExecution of the started and signaled workflow.
    */
  inline def start[A](inline f: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    ${ ZWorkflowStubSignalSyntax.signalWithStartImpl[A]('self, 'f) }
}

object ZWorkflowStubSignalSyntax {
  def signalImpl(f: Expr[Unit])(using q: Quotes): Expr[TemporalIO[TemporalClientError, Unit]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*
    val invocation = getMethodInvocation(betaReduceExpression(f).asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)

    method.assertSignalMethod()
    val signalName = getSignalName(method.symbol)

    val theMethod = invocation.instance
      .select(invocation.instance.symbol.methodMember("__zio_temporal_invokeSignal").head)

    val invokeTree =
      Apply(
        theMethod,
        List(
          Literal(StringConstant(signalName)),
          Expr.ofList(invocation.args.map(_.asExprOf[AnyRef])).asTerm
        )
      )

    invokeTree
      .asExprOf[TemporalIO[TemporalClientError, Unit]]
      .debugged(SharedCompileTimeMessages.generatedSignal)
  }

  def signalWithStartBuilderImpl(self: Expr[ZWorkflowClient], f: Expr[Unit])(using q: Quotes): Expr[ZSignalBuilder] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*
    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    method.assertSignalMethod()

    '{ new ZSignalBuilder($self, TemporalWorkflowFacade.addToBatchRequest(() => $fTree)) }
      .debugged(SharedCompileTimeMessages.generatedZSignalBuilder)
  }

  def signalWithStartImpl[A: Type](
    self:    Expr[ZSignalBuilder],
    f:       Expr[A]
  )(using q: Quotes
  ): Expr[TemporalIO[TemporalClientError, ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*
    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod("Workflow method should not be an extension method!")

    method.assertWorkflowMethod()

    // TODO: try to deconstruct self so that ZSignalBuilder won't be allocated
    val batchRequestTree = '{
      val javaClient   = $self.__zio_temporal_workflowClient.toJava
      val batchRequest = javaClient.newSignalWithStartRequest()
      TemporalWorkflowFacade.addToBatchRequest(batchRequest, () => $fTree)
      $self.__zio_temporal_addSignal(batchRequest)
      new ZWorkflowExecution(javaClient.signalWithStart(batchRequest))
    }

    '{
      zio.temporal.internal.TemporalInteraction.from {
        $batchRequestTree
      }
    }.debugged(SharedCompileTimeMessages.generatedSignalWithStart)
  }
}
