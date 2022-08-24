package zio.temporal.protobuf

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
}
