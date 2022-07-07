package zio.temporal.protobuf

/** Thrown in case of deserialization errors */
class ScalapbPayloadException private[protobuf](msg: String) extends Exception(msg)
