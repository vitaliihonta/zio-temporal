package zio.temporal.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.temporal.common.converter._

object JacksonDataConverter {
  val DefaultObjectMapperBuilder: JsonMapper.Builder =
    JsonMapper
      .builder()
      .addModule(DefaultScalaModule)
      .addModule(new JavaTimeModule)
      .addModule(BoxedUnitModule)

  val DefaultObjectMapper: ObjectMapper =
    DefaultObjectMapperBuilder.build()

  def make(objectMapper: ObjectMapper = DefaultObjectMapper): DataConverter = {
    new DefaultDataConverter(
      // order matters!
      Seq(
        new NullPayloadConverter(),
        new ByteArrayPayloadConverter(),
        new ProtobufJsonPayloadConverter(),
        new JacksonJsonPayloadConverter(objectMapper)
      ): _*
    )
  }
}
