package zio.temporal.protobuf

import scala.reflect.Enum
import scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import scala.quoted.*
import zio.temporal.internal.MacroUtils

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
  companion:       GeneratedEnumCompanion[P],
  enumName:        String,
  valueOfFunction: String => E)
    extends ProtoType[E] {

  override type Repr = P

  override def repr(value: E): P =
    companion
      .fromName(value.toString)
      .getOrElse(
        throw new EnumeratumEnumException(
          enumName,
          value.toString,
          companion
        )
      )

  override def fromRepr(repr: P): E =
    try valueOfFunction(repr.name)
    catch {
      case _: IllegalArgumentException =>
        throw new NoSuchElementException(s"$repr is not a member of enum $enumName")
    }
}

final class EnumProtoTypePartiallyApplied[P <: GeneratedEnum](private val companion: GeneratedEnumCompanion[P])
    extends AnyVal {

  inline def to[E <: Enum]: ProtoType.Of[E, P] =
    ${ EnumProtoTypePartiallyApplied.enumProtoTypeImpl[P, E]('companion) }
}

object EnumProtoTypePartiallyApplied {
  def enumProtoTypeImpl[P <: GeneratedEnum: Type, E <: Enum: Type](
    companion: Expr[GeneratedEnumCompanion[P]]
  )(using q:   Quotes
  ): Expr[ProtoType.Of[E, P]] = {
    import q.reflect.*
    val macroUtils = new MacroUtils[q.type]
    import macroUtils.*
    val tpe          = TypeRepr.of[E]
    val enumName     = tpe.show
    val enumClassSym = tpe.classSymbol.getOrElse(error(s"$enumName is not a enum!"))

    val valueOfSym = enumClassSym.companionClass
      .methodMember("valueOf")
      .headOption
      .getOrElse(error(s"$enumName companion object doesn't have valueOf method"))

    val parse = Lambda(
      Symbol.spliceOwner,
      MethodType(
        List("v")
      )(
        _ => List(TypeRepr.of[String]),
        _ => tpe
      ),
      (_, params) => Apply(companionObjectOf(tpe).select(valueOfSym), params.map(_.asExpr.asTerm))
    ).asExprOf[String => E]

    '{
      new EnumProtoType[P, E](
        $companion,
        ${
          Expr(enumName)
        },
        $parse
      )
    }.debugged("Generated EnumProtoType")
  }
}
