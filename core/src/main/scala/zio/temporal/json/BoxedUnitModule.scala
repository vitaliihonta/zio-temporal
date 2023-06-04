package zio.temporal.json

import com.fasterxml.jackson.core.{JsonGenerator, JsonParser, JsonToken}
import com.fasterxml.jackson.databind._
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.jsontype.{TypeDeserializer, TypeSerializer}
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import scala.runtime.BoxedUnit

object BoxedUnitModule extends BoxedUnitModule {
  override def getModuleName = "BoxedUnitModule"
}

class BoxedUnitModule extends SimpleModule {
  addSerializer(classOf[BoxedUnit], new BoxedUnitSerializer)
  addDeserializer(classOf[BoxedUnit], new BoxedUnitDeserializer())
}

// Serializer
private class BoxedUnitSerializer extends StdSerializer[BoxedUnit](classOf[BoxedUnit]) {
  override def serialize(value: BoxedUnit, jgen: JsonGenerator, provider: SerializerProvider): Unit = {
    provider.defaultSerializeNull(jgen)
  }

  override def serializeWithType(
    value:    BoxedUnit,
    jgen:     JsonGenerator,
    provider: SerializerProvider,
    typeSer:  TypeSerializer
  ): Unit = {
    provider.defaultSerializeNull(jgen)
  }
}

// Deserializer
private class BoxedUnitDeserializer extends StdDeserializer[BoxedUnit](classOf[BoxedUnit]) {
  private def deserializeUnit(jp: JsonParser, ctxt: DeserializationContext): BoxedUnit = {
    jp.currentToken() match {
      case JsonToken.VALUE_NULL =>
        BoxedUnit.UNIT
      case _ => ctxt.handleUnexpectedToken(classOf[BoxedUnit], jp).asInstanceOf[BoxedUnit]
    }
  }

  override def deserialize(jp: JsonParser, ctxt: DeserializationContext): BoxedUnit =
    deserializeUnit(jp, ctxt)

  override def deserializeWithType(
    jp:               JsonParser,
    ctxt:             DeserializationContext,
    typeDeserializer: TypeDeserializer
  ): BoxedUnit =
    deserializeUnit(jp, ctxt)
}
