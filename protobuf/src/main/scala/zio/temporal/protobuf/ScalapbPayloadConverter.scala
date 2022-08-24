package zio.temporal.protobuf

import com.google.protobuf.ByteString
import com.google.protobuf.`type`.TypeProto
import com.google.protobuf.any.Any
import com.google.protobuf.any.AnyProto
import com.google.protobuf.duration.DurationProto
import com.google.protobuf.empty.EmptyProto
import com.google.protobuf.field_mask.FieldMaskProto
import com.google.protobuf.struct.StructProto
import com.google.protobuf.timestamp.TimestampProto
import com.google.protobuf.wrappers.WrappersProto
import io.temporal.api.common.v1.Payload
import io.temporal.common.converter.PayloadConverter
import scalapb.GeneratedFileObject
import scalapb.GeneratedMessage
import scalapb.GeneratedMessageCompanion
import scalapb.options.ScalapbProto

import java.lang.reflect.Type
import java.nio.charset.StandardCharsets
import java.{util => ju}

/** Used to deserialize protobuf generated types */
class ScalapbPayloadConverter(files: Seq[GeneratedFileObject]) extends PayloadConverter {

  private val temporalZioPrefix = "types/zio-temporal"

  private def dropTemporalZioPrefix(typeUrl: String): String =
    if (typeUrl.startsWith(temporalZioPrefix + "/")) typeUrl.drop(temporalZioPrefix.length + 1)
    else typeUrl

  private val stdFiles: Seq[GeneratedFileObject] = Seq(
    AnyProto,
    DurationProto,
    EmptyProto,
    StructProto,
    FieldMaskProto,
    TimestampProto,
    TypeProto,
    WrappersProto,
    ScalapbProto,
    ZioTemporalProto
  )

  private val companions = (stdFiles ++ files)
    .flatMap(_.messagesCompanions)
    .map { companion =>
      companion.scalaDescriptor.fullName -> widen(companion)
    }
    .toMap

  override val getEncodingType: String = "binary/protobuf"
  private val encodingMetaKey          = "encoding"
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
      .putMetadata(encodingMetaKey, encodingMetaValue)
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
    getCompanion(content, dropTemporalZioPrefix(valueType.getTypeName))
      .parseFrom(content.getData.newCodedInput()) match {
      case optional: Optional =>
        optional.value match {
          case None => None.asInstanceOf[T]
          case Some(value) =>
            Some(
              getCompanion(content, dropTemporalZioPrefix(value.typeUrl))
                .parseFrom(value.value.newCodedInput())
            ).asInstanceOf[T]
        }
      case result: Result =>
        result.result match {
          case Result.Result.Error(error) =>
            val decoded = getCompanion(content, dropTemporalZioPrefix(error.typeUrl))
              .parseFrom(error.value.newCodedInput())

            val result = if (decoded.isInstanceOf[ZUnit]) () else decoded

            Left(result).asInstanceOf[T]

          case Result.Result.Value(value) =>
            val decoded = getCompanion(content, dropTemporalZioPrefix(value.typeUrl))
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

  private def getCompanion(content: Payload, typeUrl: String): GeneratedMessageCompanion[GeneratedMessage] =
    if (isEither(typeUrl)) widen(Result.messageCompanion)
    else if (isOption(typeUrl)) widen(Optional.messageCompanion)
    else
      companions.getOrElse(
        typeUrl,
        throw new ProtobufPayloadException(s"Unable to convert $content to $typeUrl")
      )

  private def isEither(typeUrl: String): Boolean =
    typeUrl.startsWith("scala.util.Either")

  private def isOption(typeUrl: String): Boolean =
    typeUrl.startsWith("scala.Option")

  private def widen[A <: GeneratedMessage](
    cmp: GeneratedMessageCompanion[A]
  ): GeneratedMessageCompanion[GeneratedMessage] =
    cmp.asInstanceOf[GeneratedMessageCompanion[GeneratedMessage]]
}
