package zio.temporal.internal

import io.temporal.client.BatchRequest
import zio.temporal._
import zio.temporal.signal.ZSignalBuilder
import zio.temporal.workflow.ZWorkflowClient
import scala.reflect.macros.blackbox

class ZSignalMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val SignalMethod = typeOf[signalMethod]

  private val ZSignalBuilder  = typeOf[ZSignalBuilder].dealias
  private val BatchRequest    = typeOf[BatchRequest].dealias
  private val ZWorkflowClient = typeOf[ZWorkflowClient].dealias

  def signalWithStartBuilderImpl(f: Expr[Unit]): Tree = {
    val tree = f.tree
    val self = getPrefixOf(ZWorkflowClient)

    val invocation = getMethodInvocation(tree)
    val method     = invocation.getMethod("Signal method should not be an extension method!")

    val addSignal = addBatchRequestTree(invocation, method, typeOf[Unit])(to = None)

    q"""new $ZSignalBuilder($addSignal, $self)""".debugged(s"Generated ${ZSignalBuilder.toString}")
  }

  def signalWithStartImpl[A: WeakTypeTag](f: Expr[A]): Tree = {
    val tree       = f.tree
    val invocation = getMethodInvocation(tree)
    val method     = invocation.getMethod("Workflow method should not be an extension method!")

    val self         = getPrefixOf(ZSignalBuilder)
    val batchRequest = freshTermName("batchRequest")
    val addStart     = addBatchRequestTree(invocation, method, weakTypeOf[A])(to = Some(batchRequest))
    val builder      = freshTermName("builder")
    val javaClient   = freshTermName("javaClient")

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.from {
        val $builder = $self
        val $javaClient = $builder.__zio_temporal_workflowClient.toJava
        val $batchRequest = $javaClient.newSignalWithStartRequest()
        $builder.__zio_temporal_addSignal($batchRequest)
        $addStart
        new _root_.zio.temporal.ZWorkflowExecution($javaClient.signalWithStart($batchRequest))
      }
     """.debugged(s"Generated signalWithStart")
  }

  def signalImpl(f: Expr[Unit]): Tree = {
    val tree       = f.tree
    val invocation = getMethodInvocation(tree)
    assertWorkflow(invocation.instance.tpe)
    if (!(invocation.instance.tpe <:< typeOf[BaseCanSignal])) {
      error(s".signal should be called only on ZWorkflowStub and etc.")
    }

    val method = invocation.getMethod("Signal method should not be an extension method!")

    val signalName = getSignalName(method.symbol)

    q"""${invocation.instance}.__zio_temporal_invokeSignal($signalName, ${invocation.args})""".debugged(
      "Generated signal"
    )
  }

  // TODO: refactor and handle procedures
  private def addBatchRequestTree(
    invocation: MethodInvocation,
    method:     MethodInfo,
    ret:        Type
  )(to:         Option[TermName]
  ): Tree = {
    val f = q"""${invocation.instance}.${invocation.methodName.toTermName}"""
    println(s"method=${method.name}  tree=$f")
    val batchRequest = freshTermName("batchRequest")
    method.appliedArgs match {
      case Nil =>
        val Proc     = tq"""io.temporal.workflow.Functions.Proc"""
        val procCall = q"""( ( () => $f() ): $Proc )"""
        to.fold[Tree](ifEmpty = q"""($batchRequest: $BatchRequest) => $batchRequest.add($procCall)""") { batchRequest =>
          q""" $batchRequest.add($procCall) """
        }
      case List(first) =>
        val a      = first.tpe
        val aInput = freshTermName("a")
        if (ret =:= typeOf[Unit]) {
          val Proc1    = tq"""io.temporal.workflow.Functions.Proc1[$a]"""
          val procCall = q"""( ( ($aInput: $a) => $f($aInput) ): $Proc1 )"""
          to.fold[Tree](ifEmpty = q"""($batchRequest: $BatchRequest) => $batchRequest.add[$a]($procCall, $first)""") {
            batchRequest =>
              q"""$batchRequest.add[$a]($procCall, $first)"""
          }
        } else {
          val Func1    = tq"""io.temporal.workflow.Functions.Func1[$a, $ret]"""
          val funcCall = q"""( ( ($aInput: $a) => $f($aInput) ): $Func1 )"""
          to.fold[Tree](ifEmpty =
            q"""($batchRequest: $BatchRequest) => $batchRequest.add[$a, $ret]($funcCall, $first )"""
          ) { batchRequest =>
            q"""$batchRequest.add[$a, $ret]($funcCall, $first )"""
          }
        }
      case List(first, second) =>
        val a        = first.tpe
        val b        = second.tpe
        val aInput   = freshTermName("a")
        val bInput   = freshTermName("b")
        val Func2    = tq"""io.temporal.workflow.Functions.Func2[$a, $b, $ret]"""
        val funcCall = q"""(  ( ($aInput: $a, $bInput: $b) => $f($aInput, $bInput) ): $Func2 )"""
        to.fold[Tree](ifEmpty =
          q"""($batchRequest: $BatchRequest) => $batchRequest.add[$a, $b, $ret]($funcCall, $first, $second)"""
        ) { batchRequest =>
          q"""$batchRequest.add[$a, $b, $ret]($funcCall, $first, $second)"""
        }
      case args =>
        sys.error(s"Workflow execute with arity ${args.size} not currently implemented. Feel free to contribute!")
    }
  }

  private def getSignalName(method: Symbol): String =
    getAnnotation(method, SignalMethod).children.tail
      .collectFirst { case NamedArg(_, Literal(Constant(signalName: String))) =>
        signalName
      }
      .getOrElse(method.name.toString)
}
