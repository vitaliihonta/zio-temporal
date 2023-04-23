package zio.temporal.workflow

import zio.*
import zio.temporal.internal.ConfigurationCompanion
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

object ZWorkflowClientOptions extends ConfigurationCompanion[ZWorkflowClientOptions] {
  def withNamespace(value: String): Configure =
    configure(_.withNamespace(value))

  def withDataConverter(value: => DataConverter): Configure =
    configure(_.withDataConverter(value))

  def withInterceptors(value: WorkflowClientInterceptor*): Configure =
    configure(_.withInterceptors(value: _*))

  def withIdentity(value: String): Configure =
    configure(_.withIdentity(value))

  def withBinaryChecksum(value: String): Configure =
    configure(_.withBinaryChecksum(value))

  def withContextPropagators(value: ContextPropagator*): Configure =
    configure(_.withContextPropagators(value: _*))

  def withQueryRejectCondition(value: QueryRejectCondition): Configure =
    configure(_.withQueryRejectCondition(value))

  def transformJavaOptions(
    f: WorkflowClientOptions.Builder => WorkflowClientOptions.Builder
  ): Configure = configure(_.transformJavaOptions(f))

  private val workflowClientConfig =
    Config.string("namespace").optional ++
      Config.string("identity").optional ++
      Config.string("binaryChecksum").optional

  /** Reads config from the default path `zio.temporal.ZWorkflowClient`
    */
  val make: Layer[Config.Error, ZWorkflowClientOptions] =
    makeImpl(Nil)

  /** Allows to specify custom path for the config
    */
  def forPath(name: String, names: String*): Layer[Config.Error, ZWorkflowClientOptions] =
    makeImpl(List(name) ++ names)

  private def makeImpl(additionalPath: List[String]): Layer[Config.Error, ZWorkflowClientOptions] = {
    val config = additionalPath match {
      case Nil          => workflowClientConfig.nested("zio", "temporal", "ZWorkflowClient")
      case head :: tail => workflowClientConfig.nested(head, tail: _*)
    }
    ZLayer.fromZIO {
      ZIO.config(config).map { case (namespace, identityCfg, binaryChecksum) =>
        new ZWorkflowClientOptions(
          namespace = namespace,
          dataConverter = JacksonDataConverter.make(),
          interceptors = Nil,
          identity = identityCfg,
          binaryChecksum = binaryChecksum,
          contextPropagators = Nil,
          queryRejectCondition = None,
          javaOptionsCustomization = identity
        )
      }
    }
  }
}
