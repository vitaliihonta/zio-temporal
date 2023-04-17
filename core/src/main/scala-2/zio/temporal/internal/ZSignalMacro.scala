package zio.temporal.internal

import zio.temporal.workflow.ZWorkflowStub
import scala.reflect.macros.blackbox

class ZSignalMacro(override val c: blackbox.Context) extends InvocationMacroUtils(c) {
  import c.universe._

  private val ZWorkflowStub = typeOf[ZWorkflowStub].dealias

  def signalWithStartImpl(start: Expr[Unit], signal: Expr[Unit]): Tree = {
    val self = getPrefixOf(ZWorkflowStub)

    val startInvocation = getMethodInvocation(start.tree)
    val startMethod     = startInvocation.getMethod(SharedCompileTimeMessages.wfMethodShouldntBeExtMethod)
    startMethod.assertWorkflowMethod()

    val signalInvocation = getMethodInvocation(signal.tree)
    val signalMethod     = signalInvocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    signalMethod.assertSignalMethod()

    val signalName = getSignalName(signalMethod.symbol)

    val signalWithStartTree = createSignalWithStartTree(
      self = self,
      signalName = signalName,
      signalArgs = signalMethod.appliedArgs,
      startArgs = startMethod.appliedArgs
    )

    q"""
      _root_.zio.temporal.internal.TemporalInteraction.from {
        $signalWithStartTree
      }
    """.debugged(SharedCompileTimeMessages.generatedSignalWithStart)
  }

  def signalImpl(f: Expr[Unit]): Tree = {
    val tree       = f.tree
    val invocation = getMethodInvocation(tree)
    assertWorkflow(invocation.instance.tpe)

    val method = invocation.getMethod(SharedCompileTimeMessages.sgnlMethodShouldntBeExtMethod)
    method.assertSignalMethod()
    val signalName = getSignalName(method.symbol)

    q"""
       _root_.zio.temporal.internal.TemporalInteraction.from {
         _root_.zio.temporal.internal.TemporalWorkflowFacade.signal(
           ${invocation.instance}.toJava,
           $signalName,
           ${invocation.args}
         )
       }
       """
      .debugged(SharedCompileTimeMessages.generatedSignal)
  }

  private def createSignalWithStartTree(
    self:       Tree,
    signalName: String,
    signalArgs: List[Tree],
    startArgs:  List[Tree]
  ): Tree = {
    q"""
     new _root_.zio.temporal.ZWorkflowExecution(
       _root_.zio.temporal.internal.TemporalWorkflowFacade.signalWithStart(
         $self.toJava,
         $signalName,
         Array(..$signalArgs),
         Array(..$startArgs)
       )
     )
   """
  }
}
