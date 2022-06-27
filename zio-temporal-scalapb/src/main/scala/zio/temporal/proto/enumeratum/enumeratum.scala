package zio.temporal.proto.enumeratum

import _root_.enumeratum.Enum
import _root_.enumeratum.EnumEntry
import scalapb.GeneratedEnum
import scalapb.GeneratedEnumCompanion
import zio.temporal.proto.ProtoType

final class EnumeratumEnumException[E <: EnumEntry, P <: GeneratedEnum] private[proto] (
  enum:      Enum[E],
  entry:     E,
  companion: GeneratedEnumCompanion[P])
    extends RuntimeException {

  override def getMessage: String =
    s"Unable to convert $entry (value of $enum) to $companion, expected one of ${companion.values.mkString("[", ", ", "]")}"
}

final class EnumProtoType[P <: GeneratedEnum, E <: EnumEntry] private[proto] (
  companion: GeneratedEnumCompanion[P],
  enum:      Enum[E])
    extends ProtoType[E] {

  override type Repr = P

  override def repr(value: E): P =
    companion
      .fromName(value.entryName)
      .getOrElse(
        throw new EnumeratumEnumException[E, P](enum, value, companion)
      )

  override def fromRepr(repr: P): E =
    enum.withNameInsensitive(repr.name)
}

final class EnumProtoTypePartiallyApplied[P <: GeneratedEnum](private val companion: GeneratedEnumCompanion[P])
    extends AnyVal {

  def apply[E <: EnumEntry](enum: Enum[E]): ProtoType.Of[E, P] =
    new EnumProtoType[P, E](companion, `enum`)
}

trait EnumeratumProtoType {

  def apply[P <: GeneratedEnum](companion: GeneratedEnumCompanion[P]): EnumProtoTypePartiallyApplied[P] =
    new EnumProtoTypePartiallyApplied[P](companion)
}
