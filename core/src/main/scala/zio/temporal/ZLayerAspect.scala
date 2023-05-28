package zio.temporal

import zio.*

/** Copy-paste of [[ZIOAspect]], but for [[ZLayer]]. Intended to use with various temporal's options in order to
  * override them
  */
trait ZLayerAspect[+LowerRIn, -UpperRIn, +LowerE, -UpperE, +LowerROut, -UpperROut] { self =>

  def apply[RIn >: LowerRIn <: UpperRIn, E >: LowerE <: UpperE, ROut >: LowerROut <: UpperROut](
    layer:          ZLayer[RIn, E, ROut]
  )(implicit trace: Trace
  ): ZLayer[RIn, E, ROut]

  /** Returns a new aspect that represents the sequential composition of this aspect with the specified one.
    */
  def @@[
    LowerRIn1 >: LowerRIn,
    UpperRIn1 <: UpperRIn,
    LowerE1 >: LowerE,
    UpperE1 <: UpperE,
    LowerROut1 >: LowerROut,
    UpperROut1 <: UpperROut
  ](that: ZLayerAspect[LowerRIn1, UpperRIn1, LowerE1, UpperE1, LowerROut1, UpperROut1]
  ): ZLayerAspect[LowerRIn1, UpperRIn1, LowerE1, UpperE1, LowerROut1, UpperROut1] =
    self.andThen(that)

  def andThen[
    LowerRIn1 >: LowerRIn,
    UpperRIn1 <: UpperRIn,
    LowerE1 >: LowerE,
    UpperE1 <: UpperE,
    LowerROut1 >: LowerROut,
    UpperROut1 <: UpperROut
  ](that: ZLayerAspect[LowerRIn1, UpperRIn1, LowerE1, UpperE1, LowerROut1, UpperROut1]
  ): ZLayerAspect[LowerRIn1, UpperRIn1, LowerE1, UpperE1, LowerROut1, UpperROut1] =
    new ZLayerAspect[LowerRIn1, UpperRIn1, LowerE1, UpperE1, LowerROut1, UpperROut1] {
      def apply[R >: LowerRIn1 <: UpperRIn1, E >: LowerE1 <: UpperE1, A >: LowerROut1 <: UpperROut1](
        layer:          ZLayer[R, E, A]
      )(implicit trace: Trace
      ): ZLayer[R, E, A] =
        that(self(layer))
    }
}

object ZLayerAspect {
  final class Syntax[RIn, E, ROut](private val self: ZLayer[RIn, E, ROut]) extends AnyVal {
    def @@[LowerR <: UpperR, UpperR <: RIn, LowerE >: E, UpperE >: LowerE, LowerA >: ROut, UpperA >: LowerA](
      aspect:         => ZLayerAspect[LowerR, UpperR, LowerE, UpperE, LowerA, UpperA]
    )(implicit trace: Trace
    ): ZLayer[UpperR, LowerE, LowerA] =
      ZLayer.suspend(aspect(self))
  }
}
