package zio.temporal.protobuf

import scala.reflect.Enum
import scalapb.{GeneratedEnum, GeneratedEnumCompanion}
import scala.quoted.*
import zio.temporal.internal.MacroUtils

// TODO: implement
object EnumProtoType {
  def apply[P <: GeneratedEnum](companion: GeneratedEnumCompanion[P]): EnumProtoTypePartiallyApplied[P] =
    new EnumProtoTypePartiallyApplied[P](companion)
}

final class EnumeratumEnumException[P <: GeneratedEnum] private[protobuf] (
  scalaEnumClass: String,
  entry:          String,
  companion:      GeneratedEnumCompanion[P])
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
        throw new EnumeratumEnumException[P](
          enumName,
          value.toString,
          companion
        )
      )

  override def fromRepr(repr: P): E =
    valueOfFunction(repr.name)
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
    val tpe                = TypeRepr.of[E]
    val enumName           = tpe.show
    val enumClassSym       = tpe.classSymbol.getOrElse(error(s"$enumName is not a enum!"))
    val companionObjectSym = enumClassSym.companionClass
    val companionObject    = companionObjectSym.tree.asExpr.asTerm
    val valueOfSym         = companionObjectSym.methodMember("valueOf").head

    // TODO: implement properly
    val parse = Select(companionObject, valueOfSym).asExprOf[String => E]

    '{
      new EnumProtoType[P, E](
        $companion,
        ${ Expr(enumName) },
        (v: String) => $parse(v)
      )
    }.debugged("Generated EnumProtoType")
  }
}
