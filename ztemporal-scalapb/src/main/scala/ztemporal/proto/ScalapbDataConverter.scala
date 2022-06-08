package ztemporal.proto

import io.temporal.common.converter.ByteArrayPayloadConverter
import io.temporal.common.converter.DataConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.common.converter.NullPayloadConverter
import io.temporal.common.converter.ProtobufJsonPayloadConverter
import scalapb.GeneratedFileObject

object ScalapbDataConverter {

  /** Creates data converted supporting given scalapb generated types
    * @param files generated scalapb files
    * @return a [[DataConverter]] supporting given scalapb types
    */
  def make(files: Seq[GeneratedFileObject]): DataConverter =
    new DefaultDataConverter(
      // order matters!
      Seq(
        new NullPayloadConverter(),
        new ByteArrayPayloadConverter(),
        new ProtobufJsonPayloadConverter(),
        new ScalapbPayloadConverter(files),
        new JacksonJsonPayloadConverter() // falling back to jackson for primitive types
      ): _*
    )
}
