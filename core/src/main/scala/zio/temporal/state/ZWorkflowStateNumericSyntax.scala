package zio.temporal.state

import scala.language.implicitConversions

trait ZWorkflowStateNumericSyntax {

  implicit def ZWorkflowStateNumericOps[N: Numeric](self: ZWorkflowState[N]): ZWorkflowStateNumericOps[N] =
    new ZWorkflowStateNumericOps[N](self)
}

final class ZWorkflowStateNumericOps[N](val self: ZWorkflowState[N])(implicit N: Numeric[N]) {
  def +=(num: N): self.type =
    self.update(N.plus(_, num))

  def -=(num: N): self.type =
    self.update(N.minus(_, num))

  def *=(num: N): self.type =
    self.update(N.times(_, num))
}
