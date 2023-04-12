package zio.temporal.signal

import io.temporal.client.BatchRequest
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.internalApi
import zio.temporal.workflow.ZWorkflowClient
import scala.quoted.*
import zio.temporal.internal.{InvocationMacroUtils, SharedCompileTimeMessages, TemporalWorkflowFacade}

trait ZWorkflowStubSignalSyntax {
  inline def signal(inline f: Unit): TemporalIO[Unit] =
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
  inline def start[A](inline f: A): TemporalIO[ZWorkflowExecution] =
    ${ ZWorkflowStubSignalSyntax.signalWithStartImpl[A]('self, 'f) }
}

object ZWorkflowStubSignalSyntax {
  private val ZSignalBuilderType         = "ZSignalBuilder"
  private val TemporalWorkflowFacadeType = "TemporalWorkflowFacade"
  private val Init                       = "<init>"
  private val AddToBatchRequest          = "addToBatchRequest"

  def signalImpl(f: Expr[Unit])(using q: Quotes): Expr[TemporalIO[Unit]] = {
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
      .asExprOf[TemporalIO[Unit]]
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
  }

  def signalWithStartImpl[A: Type](
    self:    Expr[ZSignalBuilder],
    f:       Expr[A]
  )(using q: Quotes
  ): Expr[TemporalIO[ZWorkflowExecution]] = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*
    val fTree      = betaReduceExpression(f)
    val invocation = getMethodInvocation(fTree.asTerm)
    val method     = invocation.getMethod("Workflow method should not be an extension method!")

    method.assertWorkflowMethod()

    val (workflowClient, addToBatchRequestExpr) = extractWorkflowClient(self)
    val batchRequestTree = addToBatchRequestExpr match {
      case Right(directCall: Expr[Any]) =>
        '{
          val javaClient   = $workflowClient.toJava
          val batchRequest = javaClient.newSignalWithStartRequest()
          TemporalWorkflowFacade.addToBatchRequest(batchRequest, () => $fTree)
          TemporalWorkflowFacade.addToBatchRequest(batchRequest, () => $directCall)
          new ZWorkflowExecution(javaClient.signalWithStart(batchRequest))
        }
      case Left(addToBatchRequest: Expr[BatchRequest => Unit]) =>
        '{
          val javaClient   = $workflowClient.toJava
          val batchRequest = javaClient.newSignalWithStartRequest()
          TemporalWorkflowFacade.addToBatchRequest(batchRequest, () => $fTree)
          $addToBatchRequest(batchRequest)
          new ZWorkflowExecution(javaClient.signalWithStart(batchRequest))
        }
    }

    '{
      zio.temporal.internal.TemporalInteraction.from {
        $batchRequestTree
      }
    }.debugged(SharedCompileTimeMessages.generatedSignalWithStart)
  }

  private def extractWorkflowClient(
    self:    Expr[ZSignalBuilder]
  )(using q: Quotes
  ): (Expr[ZWorkflowClient], Either[Expr[BatchRequest => Unit], Expr[Any]]) = {
    import q.reflect.*
    val macroUtils = new InvocationMacroUtils[q.type]
    import macroUtils.*

    betaReduceExpression(self).asTerm match {
      // Try to extract AST previously generated by startWith macro.
      // This should avoid instantiation of ZSignalBuilder in runtime by eliminating it from AST
      // For some reason, pattern matching Ident doesn't work...
      case Typed(Apply(Select(New(newWhat), Init), List(workflowClient, addToBatchRequest)), _)
          if newWhat.isInstanceOf[Ident] && newWhat.asInstanceOf[Ident].name == ZSignalBuilderType =>
        addToBatchRequest match {
          case Apply(
                Select(facade, AddToBatchRequest),
                List(
                  Block(List(DefDef(_, List(Nil) /*No params*/, _, Some(body))), _)
                )
              ) if facade.isInstanceOf[Ident] && facade.asInstanceOf[Ident].name == TemporalWorkflowFacadeType =>
            workflowClient.asExprOf[ZWorkflowClient] -> Right(body.asExprOf[Any])
          case _ =>
            // a bit less optimized
            workflowClient.asExprOf[ZWorkflowClient] -> Left(addToBatchRequest.asExprOf[BatchRequest => Unit])
        }
      // Produce non-optimized tree in case of mismatch
      case other =>
        warning(
          SharedCompileTimeMessages.zsignalBuildNotExtracted(
            other.getClass,
            other.toString
          )
        )
        '{ $self.__zio_temporal_workflowClient } -> Left('{ $self.__zio_temporal_addSignal })
    }
  }
}
