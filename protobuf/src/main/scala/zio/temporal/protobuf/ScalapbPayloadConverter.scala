package zio.temporal.protobuf

import com.google.protobuf.ByteString
import com.google.protobuf.any.Any
import io.temporal.api.common.v1.Payload
import io.temporal.common.converter.{EncodingKeys, PayloadConverter}
import scalapb.{GeneratedMessage, GeneratedMessageCompanion, GeneratedSealedOneof}
import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.{util => ju}
import scala.util.Try
import scala.collection.concurrent.TrieMap

/** Used to deserialize protobuf generated types */
class ScalapbPayloadConverter extends PayloadConverter {

  private val temporalZioPrefix = "types/zio-temporal"
  private val scalaModuleField  = "MODULE$"

  private def dropTemporalZioPrefix(typeUrl: String): String =
    if (typeUrl.startsWith(temporalZioPrefix + "/")) typeUrl.drop(temporalZioPrefix.length + 1)
    else typeUrl

  override val getEncodingType: String = "binary/protobuf"
  private val encodingMetaValue        = ByteString.copyFrom(getEncodingType, StandardCharsets.UTF_8)

  override def toData(value: scala.Any): ju.Optional[Payload] =
    value match {
      case _: scala.runtime.BoxedUnit =>
        ju.Optional.of(writeUnit())
      case scala.Right(unit) if unit.isInstanceOf[scala.runtime.BoxedUnit] =>
        ju.Optional.of(writeRight(ZUnit()))

      case scala.Right(value) if value.isInstanceOf[GeneratedMessage] =>
        ju.Optional.of(writeRight(value.asInstanceOf[GeneratedMessage]))

      case scala.Left(error) if error.isInstanceOf[GeneratedMessage] =>
        ju.Optional.of(writeLeft(error.asInstanceOf[GeneratedMessage]))

      case opt: Option[_] if opt.forall(_.isInstanceOf[scala.runtime.BoxedUnit]) =>
        ju.Optional.of(
          writeOption(opt.map(_ => ZUnit()))
        )

      case opt: Option[_] if opt.forall(_.isInstanceOf[GeneratedMessage]) =>
        ju.Optional.of(
          writeOption(opt.asInstanceOf[Option[GeneratedMessage]])
        )

      case msg: GeneratedMessage =>
        ju.Optional.of(writeGeneratedMessage(msg))

      case _ => ju.Optional.empty()
    }

  private def writeGeneratedMessage(msg: GeneratedMessage): Payload =
    Payload
      .newBuilder()
      .putMetadata(EncodingKeys.METADATA_ENCODING_KEY, encodingMetaValue)
      .putMetadata(
        EncodingKeys.METADATA_MESSAGE_TYPE_KEY,
        ByteString.copyFrom(msg.companion.scalaDescriptor.fullName, StandardCharsets.UTF_8)
      )
      .setData(msg.toByteString)
      .build()

  private def writeLeft(error: GeneratedMessage): Payload =
    writeGeneratedMessage(Result.of(Result.Result.Error(Any.pack(error, temporalZioPrefix))))

  private def writeRight(value: GeneratedMessage): Payload =
    writeGeneratedMessage(Result.of(Result.Result.Value(Any.pack(value, temporalZioPrefix))))

  private def writeOption(opt: Option[GeneratedMessage]): Payload =
    writeGeneratedMessage(Optional.of(opt.map(value => Any.pack(value, temporalZioPrefix))))

  private def writeUnit(): Payload =
    writeGeneratedMessage(ZUnit())

  override def fromData[T](content: Payload, valueClass: Class[T], valueType: Type): T =
    getCompanion(
      content,
      content.getMetadataOrThrow(EncodingKeys.METADATA_MESSAGE_TYPE_KEY).toStringUtf8,
      valueClass
    ).parseFrom(
      content.getData.newCodedInput()
    ) match {
      case optional: Optional =>
        optional.value match {
          case None => None.asInstanceOf[T]
          case Some(value) =>
            Some(
              getCompanion(content, dropTemporalZioPrefix(value.typeUrl), valueClass)
                .parseFrom(value.value.newCodedInput())
            ).asInstanceOf[T]
        }
      case result: Result =>
        result.result match {
          case Result.Result.Error(error) =>
            val decoded = getCompanion(content, dropTemporalZioPrefix(error.typeUrl), valueClass)
              .parseFrom(error.value.newCodedInput())

            val result = if (decoded.isInstanceOf[ZUnit]) () else decoded

            Left(result).asInstanceOf[T]

          case Result.Result.Value(value) =>
            val decoded = getCompanion(content, dropTemporalZioPrefix(value.typeUrl), valueClass)
              .parseFrom(value.value.newCodedInput())

            val result = if (decoded.isInstanceOf[ZUnit]) () else decoded

            Right(result).asInstanceOf[T]

          case Result.Result.Empty =>
            throw new ProtobufPayloadException(
              s"Received Either.Result.Empty while parsing $content, expected $valueType"
            )
        }

      case _: ZUnit => ().asInstanceOf[T]
      case value    => value.asInstanceOf[T]
    }

  import ScalapbPayloadConverter.CacheKey

  private val companionsCache = TrieMap.empty[CacheKey, GeneratedMessageCompanion[GeneratedMessage]]

  private def getCompanion[T](
    content:    Payload,
    typeUrl:    String,
    valueClass: Class[T]
  ): GeneratedMessageCompanion[GeneratedMessage] = {
    companionsCache.getOrElseUpdate(
      CacheKey(typeUrl, valueClass),
      getCompanionImpl(content, typeUrl, valueClass)
    )
  }

  private def getCompanionImpl[T](
    content:    Payload,
    typeUrl:    String,
    valueClass: Class[T]
  ): GeneratedMessageCompanion[GeneratedMessage] = {

    val tryValueClassCompanion = {
      // Special case 1: ScalaPB-generated oneOf via sealed trait. It requires a little different class lookup
      if (valueClass.isInterface && valueClass.getInterfaces.contains(classOf[GeneratedSealedOneof])) {
        val subClassType = typeUrl.split("\\.").last
        val subClassName = valueClass.getName.replace(valueClass.getSimpleName, subClassType)
        getClassCompanion(Class.forName(subClassName))
      } else {
        // Regular case
        getClassCompanion(valueClass)
      }
    }

    tryValueClassCompanion
      .orElse {
        // Special case 2: Either, Option and Unit are converted into zio-temporal-provided wrappers.
        // While the valueClass refers to them, the actual bytes must be encoded into wrappers.
        Try(Class.forName(typeUrl))
          .flatMap(getClassCompanion(_))
      }
      .getOrElse {
        throw new ProtobufPayloadException(s"Unable to convert $content to $valueClass")
      }
  }

  private def getClassCompanion[T](valueClass: Class[T]): Try[GeneratedMessageCompanion[GeneratedMessage]] = {
    Try {
      val companionClass = Class.forName(valueClass.getName + "$")
      companionClass
        .getDeclaredField(scalaModuleField)
        .get(null)
        // it may fail if the class is not protobuf generated
        .asInstanceOf[GeneratedMessageCompanion[GeneratedMessage]]
    }
  }
}

object ScalapbPayloadConverter {
  protected final case class CacheKey(typeUrl: String, valueClass: Class[_])
}
