package ztemporal.signal

import izumi.reflect.Tag
import izumi.reflect.macrortti.LightTypeTag
import scala.annotation.implicitNotFound

/** Represents [[ZSignal]] input type.
  *
  * Inspired by [[zio.Has]]
  */
final class ZInput[A] private (private[ztemporal] val inputs: Map[LightTypeTag, Any]) extends Serializable {
  def get(tag: LightTypeTag): Any = inputs(tag)

  def args: Seq[Any] = inputs.values.toSeq
}

object ZInput {
  private[ztemporal] val empty: ZInput[Any] = new ZInput(Map.empty)

  private[ztemporal] def apply[A: Tag](value: A): ZInput[A] = new ZInput[A](Map(Tag[A].tag -> value))

  final implicit class ZInputSyntax[Self <: ZInput[_]](private val self: Self) extends AnyVal {

    /** Combines this ZInput with the specified one.
      */
    def ++[B <: ZInput[_]](that: B): Self with B =
      new ZInput[Any](self.inputs ++ that.inputs).asInstanceOf[Self with B]
  }

  @implicitNotFound("Value of type ${A} cannot be converted to ZInput")
  sealed trait From[A] {
    def apply(value: A): ZInput[_]
  }

  final object From {

    implicit val fromAny: From[Any]                 = FromAny
    implicit def fromInput[A <: ZInput[_]]: From[A] = new FromInput[A]

    final object FromAny extends From[Any] {
      override def apply(value: Any): ZInput[_] = empty
    }

    final class FromInput[A <: ZInput[_]] extends From[A] {
      override def apply(value: A): ZInput[_] = value
    }
  }
}
