package zio.temporal

import java.lang.reflect.{ParameterizedType, Type}
import scala.annotation.implicitNotFound
import scala.reflect.ClassTag

/** It's used to provide type hints of parameterized types for [[io.temporal.common.converter.PayloadConverter]] when
  * obtaining Activity result, Workflow result, etc.
  *
  * @note
  *   currently supports types with up to 7 type parameters. Feel free to contribute if you need more =)
  *
  * @tparam A
  *   type to provide a hint for
  */
@implicitNotFound(
  "Cannot find implicit JavaTypeTag[${A}]. If you're using generic, please add an implicit parameter of this type"
)
trait JavaTypeTag[A] {
  def klass: Class[A]

  /** Returns as much detailed java Type as possible
    */
  def genericType: Type
}

object JavaTypeTag extends LowPriorityImplicits0 with LowPriorityImplicits1 {
  def apply[A](implicit ev: JavaTypeTag[A]): ev.type = ev
}

trait LowPriorityImplicits0 {
  implicit def kind1[Wrapper[_], A: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A]]
  ): JavaTypeTag[Wrapper[A]] = new JavaTypeTags.Kind1[Wrapper, A]

  implicit def kind2[Wrapper[_, _], A: JavaTypeTag, B: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A, B]]
  ): JavaTypeTag[Wrapper[A, B]] = new JavaTypeTags.Kind2[Wrapper, A, B]

  implicit def kind3[Wrapper[_, _, _], A: JavaTypeTag, B: JavaTypeTag, C: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C]]
  ): JavaTypeTag[Wrapper[A, B, C]] = new JavaTypeTags.Kind3[Wrapper, A, B, C]

  implicit def kind4[Wrapper[_, _, _, _], A: JavaTypeTag, B: JavaTypeTag, C: JavaTypeTag, D: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D]]
  ): JavaTypeTag[Wrapper[A, B, C, D]] = new JavaTypeTags.Kind4[Wrapper, A, B, C, D]

  implicit def kind5[
    Wrapper[_, _, _, _, _],
    A: JavaTypeTag,
    B: JavaTypeTag,
    C: JavaTypeTag,
    D: JavaTypeTag,
    E: JavaTypeTag
  ](implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D, E]]
  ): JavaTypeTag[Wrapper[A, B, C, D, E]] = new JavaTypeTags.Kind5[Wrapper, A, B, C, D, E]

  implicit def kind6[
    Wrapper[_, _, _, _, _, _],
    A: JavaTypeTag,
    B: JavaTypeTag,
    C: JavaTypeTag,
    D: JavaTypeTag,
    E: JavaTypeTag,
    F: JavaTypeTag
  ](implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D, E, F]]
  ): JavaTypeTag[Wrapper[A, B, C, D, E, F]] =
    new JavaTypeTags.Kind6[Wrapper, A, B, C, D, E, F]

  implicit def kind7[
    Wrapper[_, _, _, _, _, _, _],
    A: JavaTypeTag,
    B: JavaTypeTag,
    C: JavaTypeTag,
    D: JavaTypeTag,
    E: JavaTypeTag,
    F: JavaTypeTag,
    G: JavaTypeTag
  ](implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D, E, F, G]]
  ): JavaTypeTag[Wrapper[A, B, C, D, E, F, G]] =
    new JavaTypeTags.Kind7[Wrapper, A, B, C, D, E, F, G]
}

trait LowPriorityImplicits1 {
  implicit def kind0[A: ClassTag]: JavaTypeTag[A] = new JavaTypeTags.Kind0[A]
}

private[temporal] object JavaTypeTags {
  final class Kind0[A](implicit ctg: ClassTag[A]) extends JavaTypeTag[A] {
    override val klass: Class[A] = ctg.runtimeClass.asInstanceOf[Class[A]]

    override val genericType: Type = ctg.runtimeClass
  }

  final class Kind1[Wrapper[_], A: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A]])
      extends JavaTypeTag[Wrapper[A]] {
    override val klass: Class[Wrapper[A]] = wrapperRawCtg.runtimeClass.asInstanceOf[Class[Wrapper[A]]]

    override val genericType: Type = new ParameterizedType {
      override val getActualTypeArguments: Array[Type] =
        Array(JavaTypeTag[A].genericType)

      override val getRawType: Type =
        wrapperRawCtg.runtimeClass

      override val getOwnerType: Type =
        null
    }
  }

  final class Kind2[Wrapper[_, _], A: JavaTypeTag, B: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A, B]])
      extends JavaTypeTag[Wrapper[A, B]] {
    override val klass: Class[Wrapper[A, B]] = wrapperRawCtg.runtimeClass.asInstanceOf[Class[Wrapper[A, B]]]

    override val genericType: Type = new ParameterizedType {
      override val getActualTypeArguments: Array[Type] =
        Array(JavaTypeTag[A].genericType, JavaTypeTag[B].genericType)

      override val getRawType: Type =
        wrapperRawCtg.runtimeClass

      override val getOwnerType: Type =
        null
    }
  }

  final class Kind3[Wrapper[_, _, _], A: JavaTypeTag, B: JavaTypeTag, C: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C]])
      extends JavaTypeTag[Wrapper[A, B, C]] {

    override val klass: Class[Wrapper[A, B, C]] = wrapperRawCtg.runtimeClass.asInstanceOf[Class[Wrapper[A, B, C]]]

    override val genericType: Type = new ParameterizedType {
      override val getActualTypeArguments: Array[Type] =
        Array(JavaTypeTag[A].genericType, JavaTypeTag[B].genericType, JavaTypeTag[C].genericType)

      override val getRawType: Type =
        wrapperRawCtg.runtimeClass

      override val getOwnerType: Type =
        null
    }
  }

  final class Kind4[Wrapper[_, _, _, _], A: JavaTypeTag, B: JavaTypeTag, C: JavaTypeTag, D: JavaTypeTag](
    implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D]])
      extends JavaTypeTag[Wrapper[A, B, C, D]] {

    override val klass: Class[Wrapper[A, B, C, D]] =
      wrapperRawCtg.runtimeClass.asInstanceOf[Class[Wrapper[A, B, C, D]]]

    override val genericType: Type = new ParameterizedType {
      override val getActualTypeArguments: Array[Type] =
        Array(JavaTypeTag[A].genericType,
              JavaTypeTag[B].genericType,
              JavaTypeTag[C].genericType,
              JavaTypeTag[D].genericType
        )

      override val getRawType: Type =
        wrapperRawCtg.runtimeClass

      override val getOwnerType: Type =
        null
    }
  }

  final class Kind5[
    Wrapper[_, _, _, _, _],
    A: JavaTypeTag,
    B: JavaTypeTag,
    C: JavaTypeTag,
    D: JavaTypeTag,
    E: JavaTypeTag
  ](implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D, E]])
      extends JavaTypeTag[Wrapper[A, B, C, D, E]] {

    override val klass: Class[Wrapper[A, B, C, D, E]] =
      wrapperRawCtg.runtimeClass.asInstanceOf[Class[Wrapper[A, B, C, D, E]]]

    override val genericType: Type = new ParameterizedType {
      override val getActualTypeArguments: Array[Type] =
        Array(
          JavaTypeTag[A].genericType,
          JavaTypeTag[B].genericType,
          JavaTypeTag[C].genericType,
          JavaTypeTag[D].genericType,
          JavaTypeTag[E].genericType
        )

      override val getRawType: Type =
        wrapperRawCtg.runtimeClass

      override val getOwnerType: Type =
        null
    }
  }

  final class Kind6[
    Wrapper[_, _, _, _, _, _],
    A: JavaTypeTag,
    B: JavaTypeTag,
    C: JavaTypeTag,
    D: JavaTypeTag,
    E: JavaTypeTag,
    F: JavaTypeTag
  ](implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D, E, F]])
      extends JavaTypeTag[Wrapper[A, B, C, D, E, F]] {

    override val klass: Class[Wrapper[A, B, C, D, E, F]] =
      wrapperRawCtg.runtimeClass.asInstanceOf[Class[Wrapper[A, B, C, D, E, F]]]

    override val genericType: Type = new ParameterizedType {
      override val getActualTypeArguments: Array[Type] =
        Array(
          JavaTypeTag[A].genericType,
          JavaTypeTag[B].genericType,
          JavaTypeTag[C].genericType,
          JavaTypeTag[D].genericType,
          JavaTypeTag[E].genericType,
          JavaTypeTag[F].genericType
        )

      override val getRawType: Type =
        wrapperRawCtg.runtimeClass

      override val getOwnerType: Type =
        null
    }
  }

  final class Kind7[
    Wrapper[_, _, _, _, _, _, _],
    A: JavaTypeTag,
    B: JavaTypeTag,
    C: JavaTypeTag,
    D: JavaTypeTag,
    E: JavaTypeTag,
    F: JavaTypeTag,
    G: JavaTypeTag
  ](implicit wrapperRawCtg: ClassTag[Wrapper[A, B, C, D, E, F, G]])
      extends JavaTypeTag[Wrapper[A, B, C, D, E, F, G]] {

    override val klass: Class[Wrapper[A, B, C, D, E, F, G]] =
      wrapperRawCtg.runtimeClass.asInstanceOf[Class[Wrapper[A, B, C, D, E, F, G]]]

    override val genericType: Type = new ParameterizedType {
      override val getActualTypeArguments: Array[Type] =
        Array(
          JavaTypeTag[A].genericType,
          JavaTypeTag[B].genericType,
          JavaTypeTag[C].genericType,
          JavaTypeTag[D].genericType,
          JavaTypeTag[E].genericType,
          JavaTypeTag[F].genericType,
          JavaTypeTag[G].genericType
        )

      override val getRawType: Type =
        wrapperRawCtg.runtimeClass

      override val getOwnerType: Type =
        null
    }
  }
}
