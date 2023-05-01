package zio.temporal.internal

import org.slf4j.Logger
import scala.language.experimental.macros
import scala.reflect.macros.blackbox

trait ZWorkflowVersionSpecific {

  /** Get logger to use inside workflow. Logs in replay mode are omitted unless
    * [[zio.temporal.worker.ZWorkerFactoryOptions.enableLoggingInReplay]] is set to 'true'.
    *
    * The logger name will correspond to the enclosing class/trait name.
    *
    * @return
    *   logger to use in workflow logic.
    */
  def makeLogger: Logger =
    macro ZWorkflowVersionSpecificMacro.makeLoggerImpl
}

class ZWorkflowVersionSpecificMacro(override val c: blackbox.Context) extends MacroUtils(c) {
  import c.universe._

  def makeLoggerImpl: Expr[Logger] = {
    c.Expr[Logger](
      q"""_root_.zio.temporal.workflow.ZWorkflow.getLogger(getClass)"""
    )
  }
}
