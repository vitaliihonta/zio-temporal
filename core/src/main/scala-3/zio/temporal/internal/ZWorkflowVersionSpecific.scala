package zio.temporal.internal

import org.slf4j.Logger
import scala.quoted.*
import zio.temporal.workflow.ZWorkflow

trait ZWorkflowVersionSpecific {

  /** Get logger to use inside workflow. Logs in replay mode are omitted unless
    * [[zio.temporal.worker.ZWorkerFactoryOptions.enableLoggingInReplay]] is set to 'true'.
    *
    * The logger name will correspond to the enclosing class/trait name.
    *
    * @return
    *   logger to use in workflow logic.
    */
  inline def makeLogger: Logger =
    ${ ZWorkflowVersionSpecificMacro.getLoggerImpl }
}

object ZWorkflowVersionSpecificMacro {
  def getLoggerImpl(using q: Quotes): Expr[Logger] = {
    import q.reflect.*

    @annotation.tailrec
    def enclosingClass(symb: Symbol): Symbol =
      if (symb.isClassDef) symb else enclosingClass(symb.owner)

    val name       = enclosingClass(Symbol.spliceOwner).fullName
    val loggerName = Literal(StringConstant(name)).asExprOf[String]

    '{
      ZWorkflow.getLogger($loggerName)
    }
  }
}
