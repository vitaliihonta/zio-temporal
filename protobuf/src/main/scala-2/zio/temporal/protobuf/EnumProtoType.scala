package zio.temporal.protobuf

import enumeratum.Enum
import enumeratum.EnumEntry
import scalapb.GeneratedEnum
import scalapb.GeneratedEnumCompanion

/** Provides a conversion between scalapb generated enums and enumeratum It's an optional dependency which won't be
  * added to the classpath unless you use it
  */
object EnumProtoType {
  def apply[P <: GeneratedEnum](companion: GeneratedEnumCompanion[P]): EnumProtoTypePartiallyApplied[P] =
    new EnumProtoTypePartiallyApplied[P](companion)
}

final class EnumeratumEnumException[E <: EnumEntry, P <: GeneratedEnum] private[protobuf] (
  enum:      Enum[E],
  entry:     E,
  companion: GeneratedEnumCompanion[P])
    extends RuntimeException {

  override def getMessage: String =
    s"Unable to convert $entry (value of $enum) to $companion, expected one of ${companion.values.mkString("[", ", ", "]")}"
}

final class EnumProtoType[P <: GeneratedEnum, E <: EnumEntry] private[protobuf] (
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
