package zio.temporal.internal

import zio.temporal._
import zio.temporal.signal.ZSignal
import scala.reflect.macros.blackbox

class ZSignalMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val SignalMethod = typeOf[signalMethod]

  private val Signal          = typeOf[ZSignal.Signal].dealias
  private val SignalWithStart = typeOf[ZSignal.WithStart].dealias

  def startImpl[A](f: Expr[A]): Tree = {
    println(f)
    println(f.getClass)
    ???
  }

  def signalImpl(f: Expr[Unit]): Tree = {
    val invocation = getMethodInvocation(f.tree)

    assertWorkflow(invocation.instance.tpe)
    if (!(invocation.instance.tpe <:< typeOf[BaseCanSignal])) {
      error(s".signal should be called only on ZWorkflowStub and etc.")
    }

    val method = invocation.getMethod("Signal method should not be an extension method!")

    val signalName = getSignalName(method.symbol)

    q"""${invocation.instance}.__zio_temporal_invokeSignal(new $Signal($signalName, ${invocation.args}))""".debugged(
      "Generated signal"
    )
  }

  def signalWithStartImpl[A](f: Expr[A]): Tree = {
    println(f.tree)
    println(f.tree.getClass)
    ???
  }

  private def getSignalName(method: Symbol): String =
    getAnnotation(method, SignalMethod).children.tail
      .collectFirst { case NamedArg(_, Literal(Constant(signalName: String))) =>
        signalName
      }
      .getOrElse(method.name.toString)
}
