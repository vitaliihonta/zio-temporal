package zio.temporal.json

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.temporal.common.converter.*

object JacksonDataConverter {
  val DefaultObjectMapper: ObjectMapper =
    JsonMapper
      .builder()
      .addModule(DefaultScalaModule)
      .addModule(new JavaTimeModule)
      .build()

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
