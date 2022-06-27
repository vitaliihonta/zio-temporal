package zio.temporal.proto

/** Thrown in case of deserialization errors */
class ScalapbPayloadException private[proto] (msg: String) extends Exception(msg)
