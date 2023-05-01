package zio.temporal.protobuf.internal

import java.{util => ju}
import java.math
import zio.temporal.protobuf._
import com.google.protobuf.ByteString

private[protobuf] sealed abstract class IdType[A] extends ProtoType[A] {
  override type Repr = A

  override def repr(value: A): A = value

  override def fromRepr(repr: A): A = repr
}

private[protobuf] final object IntegerType extends IdType[Int]

private[protobuf] final object LongType extends IdType[Long]

private[protobuf] final object BooleanType extends IdType[Boolean]

private[protobuf] final object StringType extends IdType[String]

private[protobuf] final object BytesType extends IdType[Array[Byte]]

private[protobuf] final object UuidType extends ProtoType[ju.UUID] {
  override type Repr = UUID

  override def repr(value: ju.UUID): UUID =
    UUID.of(
      mostSignificantBits = value.getMostSignificantBits,
      leastSignificantBits = value.getLeastSignificantBits
    )

  override def fromRepr(repr: UUID): ju.UUID =
    new ju.UUID(repr.mostSignificantBits, repr.leastSignificantBits)
}

private[protobuf] final object BigDecimalType extends ProtoType[scala.BigDecimal] {
  override type Repr = BigDecimal

  override def repr(value: scala.BigDecimal): BigDecimal =
    BigDecimal(
      scale = value.scale,
      intVal = BigIntegerType.repr(value.bigDecimal.unscaledValue())
    )

  override def fromRepr(repr: BigDecimal): scala.BigDecimal =
    new scala.BigDecimal(new math.BigDecimal(BigIntegerType.fromRepr(repr.intVal).bigInteger, repr.scale))
}

private[protobuf] final object BigIntegerType extends ProtoType[scala.BigInt] {
  override type Repr = BigInteger

  override def repr(value: BigInt): BigInteger =
    BigInteger(value = ByteString.copyFrom(value.toByteArray))

  override def fromRepr(repr: BigInteger): BigInt =
    new BigInt(new math.BigInteger(repr.value.toByteArray))
}

private[protobuf] final object ZUnitType extends ProtoType[Unit] {
  override type Repr = ZUnit

  override def repr(value: Unit): ZUnitType.Repr = ZUnit()

  override def fromRepr(repr: ZUnitType.Repr): Unit = ()
}

private[protobuf] final class ConvertedType[A, Repr0, B](self: ProtoType.Of[A, Repr0], project: A => B, reverse: B => A)
    extends ProtoType[B] {
  override type Repr = Repr0

  override def repr(value: B): Repr0 = self.repr(reverse(value))

  override def fromRepr(repr: Repr0): B = project(self.fromRepr(repr))
}

private[protobuf] final class OptionType[A, Repr0](underlying: ProtoType.Of[A, Repr0]) extends ProtoType[Option[A]] {
  override type Repr = Option[Repr0]

  override def repr(value: Option[A]): Option[Repr0] =
    value.map(underlying.repr)

  override def fromRepr(repr: Option[Repr0]): Option[A] =
    repr.map(underlying.fromRepr)
}
