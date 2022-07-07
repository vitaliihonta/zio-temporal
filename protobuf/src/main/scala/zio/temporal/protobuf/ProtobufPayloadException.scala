package zio.temporal.protobuf

/** Thrown in case of deserialization errors */
class ProtobufPayloadException private[protobuf] (msg: String) extends Exception(msg)
