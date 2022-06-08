package ztemporal.proto

import zio.ZIO

object syntax {

  final implicit class ToProtoTypeSyntax[A](private val self: A) extends AnyVal {

    /** Converts the given value into it's protocol buffers representation */
    def toProto(implicit protoType: ProtoType[A]): protoType.Repr =
      protoType.repr(self)
  }

  final implicit class FromProtoTypeSyntax[A](private val self: A) extends AnyVal {

    /** Creates a value from it's protocol buffers representation */
    def fromProto[B](implicit protoType: ProtoType.Of[B, A]): B =
      protoType.fromRepr(self)
  }

  implicit def zioZUnitConversion[R, E](self: ZIO[R, E, Unit]): ZIO[R, E, ZUnit] =
    self.as(ZUnit())
}
