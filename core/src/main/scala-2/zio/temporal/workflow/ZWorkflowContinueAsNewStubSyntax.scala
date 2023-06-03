package zio.temporal.workflow

import zio.temporal.internal.ZWorkflowMacro
import scala.language.experimental.macros

trait ZWorkflowContinueAsNewStubSyntax {
  def execute[R](f: R): R =
    macro ZWorkflowMacro.continueAsNewImpl[R]
}
