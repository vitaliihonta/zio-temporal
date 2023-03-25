package zio.temporal.json

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.temporal.common.converter._

object JacksonDataConverter {
  def make(): DataConverter = {
    new DefaultDataConverter(
      // order matters!
      Seq(
        new NullPayloadConverter(),
        new ByteArrayPayloadConverter(),
        new ProtobufJsonPayloadConverter(),
        new JacksonJsonPayloadConverter(
          JsonMapper
            .builder()
            .addModule(DefaultScalaModule)
            .build()
        )
      ): _*
    )
  }
}
