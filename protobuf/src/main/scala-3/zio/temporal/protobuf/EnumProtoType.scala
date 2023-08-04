package zio.temporal.protobuf

import scala.reflect.Enum
import scalapb.{GeneratedEnum, GeneratedEnumCompanion}

import scala.quoted._
import zio.temporal.internal.{MacroUtils, Scala3EnumUtils}

object EnumProtoType {
  def apply[P <: GeneratedEnum](companion: GeneratedEnumCompanion[P]): EnumProtoTypePartiallyApplied[P] =
    new EnumProtoTypePartiallyApplied[P](companion)
}

final class EnumeratumEnumException private[protobuf] (
  scalaEnumClass: String,
  entry:          String,
  companion:      GeneratedEnumCompanion[_])
    extends RuntimeException {

  override def getMessage: String =
    s"Unable to convert $entry (value of $scalaEnumClass) to $companion, expected one of ${companion.values.mkString("[", ", ", "]")}"
}

final class EnumProtoType[P <: GeneratedEnum, E <: Enum](
  companion: GeneratedEnumCompanion[P],
  enumMeta:  Scala3EnumUtils.Scala3EnumMeta[E])
    extends ProtoType[E] {

  override type Repr = P

  override def repr(value: E): P =
    companion
      .fromName(value.toString)
      .getOrElse(
        throw new EnumeratumEnumException(
          enumMeta.name,
          value.toString,
          companion
        )
      )

  override def fromRepr(repr: P): E =
    try enumMeta.valueOf(repr.name)
    catch {
      case _: IllegalArgumentException =>
        throw new NoSuchElementException(s"$repr is not a member of enum ${enumMeta.name}")
    }
}

final class EnumProtoTypePartiallyApplied[P <: GeneratedEnum](private val companion: GeneratedEnumCompanion[P])
    extends AnyVal {

  inline def to[E <: Enum]: ProtoType.Of[E, P] = {
    val enumMeta = Scala3EnumUtils.getEnumMeta[E]
    new EnumProtoType[P, E](companion, enumMeta)
  }
}
