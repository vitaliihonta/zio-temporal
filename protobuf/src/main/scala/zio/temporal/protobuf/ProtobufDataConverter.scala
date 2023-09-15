package zio.temporal.protobuf

import io.temporal.common.converter._

object ProtobufDataConverter {

  /** Creates data converted supporting given protobuf generated types
    * @param files
    *   generated protobuf files
    * @return
    *   a [[DataConverter]] supporting given protobuf types
    */
  def make(): DataConverter =
    new DefaultDataConverter(
      // order matters!
      Seq(
        new NullPayloadConverter(),
        new ByteArrayPayloadConverter(),
        new ProtobufJsonPayloadConverter(),
        new ScalapbPayloadConverter(),
        new JacksonJsonPayloadConverter() // falling back to jackson for primitive types
      ): _*
    )

}
