package zio.temporal.workflow

import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import io.temporal.api.enums.v1.QueryRejectCondition
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.context.ContextPropagator
import io.temporal.common.converter.*
import io.temporal.common.interceptors.WorkflowClientInterceptor
import zio.temporal.json.JacksonDataConverter
import scala.jdk.CollectionConverters.*

/** Represents temporal workflow client options
  *
  * @see
  *   [[WorkflowClientOptions]]
  */
case class ZWorkflowClientOptions private[zio] (
  namespace:                            Option[String],
  dataConverter:                        DataConverter,
  interceptors:                         List[WorkflowClientInterceptor],
  identity:                             Option[String],
  binaryChecksum:                       Option[String],
  contextPropagators:                   List[ContextPropagator],
  queryRejectCondition:                 Option[QueryRejectCondition],
  private val javaOptionsCustomization: WorkflowClientOptions.Builder => WorkflowClientOptions.Builder) {

  def withNamespace(value: String): ZWorkflowClientOptions =
    copy(namespace = Some(value))

  def withDataConverter(value: DataConverter): ZWorkflowClientOptions =
    copy(dataConverter = value)

  def withInterceptors(value: WorkflowClientInterceptor*): ZWorkflowClientOptions =
    copy(interceptors = value.toList)

  def withIdentity(value: String): ZWorkflowClientOptions =
    copy(identity = Some(value))

  def withBinaryChecksum(value: String): ZWorkflowClientOptions =
    copy(binaryChecksum = Some(value))

  def withContextPropagators(value: ContextPropagator*): ZWorkflowClientOptions =
    copy(contextPropagators = value.toList)

  def withQueryRejectCondition(value: QueryRejectCondition): ZWorkflowClientOptions =
    copy(queryRejectCondition = Some(value))

  /** Allows to specify options directly on the java SDK's [[WorkflowClientOptions]]. Use it in case an appropriate
    * `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkflowClientOptions.Builder => WorkflowClientOptions.Builder
  ): ZWorkflowClientOptions =
    copy(javaOptionsCustomization = f)

  def toJava: WorkflowClientOptions = {
    val builder = WorkflowClientOptions.newBuilder()

    namespace.foreach(builder.setNamespace)
    builder.setDataConverter(dataConverter)
    builder.setInterceptors(interceptors: _*)
    identity.foreach(builder.setIdentity)
    binaryChecksum.foreach(builder.setBinaryChecksum)
    builder.setContextPropagators(contextPropagators.asJava)
    queryRejectCondition.foreach(builder.setQueryRejectCondition)

    javaOptionsCustomization(builder).build()
  }
}

object ZWorkflowClientOptions {

  val defaultDataConverter: DataConverter = JacksonDataConverter.make()

  val default: ZWorkflowClientOptions = new ZWorkflowClientOptions(
    namespace = None,
    dataConverter = defaultDataConverter,
    interceptors = Nil,
    identity = None,
    binaryChecksum = None,
    contextPropagators = Nil,
    queryRejectCondition = None,
    javaOptionsCustomization = identity
  )
}
