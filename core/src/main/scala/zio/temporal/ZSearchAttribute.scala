package zio.temporal

import java.time.LocalDateTime
import java.util.UUID
import java.{util => ju}
import scala.annotation.implicitNotFound
import scala.jdk.CollectionConverters.*
import scala.language.implicitConversions

/** Base type for attribute value.
  *
  * Restricted to allowed elasticsearch types.
  * @see
  *   https://docs.temporal.io/docs/server/workflow-search#search-attributes
  */
sealed trait ZSearchAttribute {
  private[zio] def attributeValue: Any

  override def toString: String = attributeValue.toString
}

object ZSearchAttribute {

  /** Converts a value to [[ZSearchAttribute]] having implicit [[Convert]] instance
    *
    * @param value
    *   attributes value to convert
    * @param convert
    *   conversion typeclass
    * @return
    *   converted search attribute
    */
  implicit def from[A](value: A)(implicit convert: Convert[A]): ZSearchAttribute =
    convert.toAttribute(value)

  /** Converts custom search attributes to [[java.util.Map]] that temporal Java SDK can consume
    *
    * @param attrs
    *   attributes to convert
    * @return
    *   attributes converted
    */
  implicit def toJava(attrs: Map[String, ZSearchAttribute]): ju.Map[String, AnyRef] =
    attrs.map { case (k, v) => k -> v.attributeValue }.asInstanceOf[Map[String, AnyRef]].asJava

  @implicitNotFound("""${A} is not a valid type for search attribute.
Consider using one of existing ZSearchAttribute's 
or provide a reasonable instance of Convert[${A}] using .contramap""")
  sealed trait Convert[A] { self =>
    def toAttribute(value: A): ZSearchAttribute

    def contramap[B](f: B => A): Convert[B]
  }

  final object Convert extends EnumSearchAttributes {
    def create[A](f: A => ZSearchAttribute): Convert[A] = new ConvertImpl[A](f)

    implicit val string: Convert[String]          = create(new StringAttr(_))
    implicit val uuid: Convert[UUID]              = string.contramap(_.toString)
    implicit val boolean: Convert[Boolean]        = create(new BooleanAttr(_))
    implicit val int: Convert[Int]                = create(new IntegralAttr(_))
    implicit val long: Convert[Long]              = create(new IntegralAttr(_))
    implicit val double: Convert[Double]          = create(new NumberAttr(_))
    implicit val dateTime: Convert[LocalDateTime] = create(new DateTimeAttr(_))
    implicit val bigDecimal: Convert[BigDecimal]  = string.contramap(_.toString)

    private class ConvertImpl[A](f: A => ZSearchAttribute) extends Convert[A] {
      override def toAttribute(value: A): ZSearchAttribute = f(value)

      override def contramap[B](f: B => A): Convert[B] =
        new ContramapImpl[A, B](this, f)
    }

    private class ContramapImpl[A, B](base: Convert[A], project: B => A) extends Convert[B] {
      override def toAttribute(value: B): ZSearchAttribute = base.toAttribute(project(value))

      override def contramap[C](f: C => B): Convert[C] =
        new ContramapImpl[A, C](base, project compose f)
    }
  }

  final class StringAttr private[zio] (override val attributeValue: String)          extends ZSearchAttribute
  final class BooleanAttr private[zio] (override val attributeValue: Boolean)        extends ZSearchAttribute
  final class IntegralAttr private[zio] (override val attributeValue: Long)          extends ZSearchAttribute
  final class NumberAttr private[zio] (override val attributeValue: Double)          extends ZSearchAttribute
  final class DateTimeAttr private[zio] (override val attributeValue: LocalDateTime) extends ZSearchAttribute
}
