package zio.temporal.workflow

import zio.*
import zio.temporal.internal.ZWorkflowMacro
import zio.temporal.*
import scala.language.experimental.macros

trait ZWorkflowContinueAsNewStubSyntax {
  def execute[R](f: R): R =
    macro ZWorkflowMacro.continueAsNewImpl[R]
}
