package zio.temporal.extras

import zio.*
import scala.language.implicitConversions

final class ZLayerAspectSyntax[RIn, E, ROut](private val self: ZLayer[RIn, E, ROut]) extends AnyVal {
  def @@[LowerR <: UpperR, UpperR <: RIn, LowerE >: E, UpperE >: LowerE, LowerA >: ROut, UpperA >: LowerA](
    aspect:         => ZLayerAspect[LowerR, UpperR, LowerE, UpperE, LowerA, UpperA]
  )(implicit trace: Trace
  ): ZLayer[UpperR, LowerE, LowerA] =
    ZLayer.suspend(aspect(self))
}

trait ZLayerAspects {
  implicit def ZLayerAspectSyntax[RIn, E, ROut](self: ZLayer[RIn, E, ROut]): ZLayerAspectSyntax[RIn, E, ROut] =
    new ZLayerAspectSyntax[RIn, E, ROut](self)
}
