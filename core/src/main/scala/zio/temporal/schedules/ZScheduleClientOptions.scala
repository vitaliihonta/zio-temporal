package zio.temporal.schedules

import io.temporal.client.schedules.ScheduleClientOptions
import io.temporal.common.context.ContextPropagator
import io.temporal.common.converter.DataConverter
import zio.temporal.internal.ConfigurationCompanion
import zio._
import scala.jdk.CollectionConverters._

/** Represents Temporal schedule client options
  *
  * @see
  *   [[ScheduleClientOptions]]
  */
case class ZScheduleClientOptions private[zio] (
  namespace:                            Option[String],
  dataConverter:                        Option[DataConverter],
  identity:                             Option[String],
  contextPropagators:                   List[ContextPropagator],
  private val javaOptionsCustomization: ScheduleClientOptions.Builder => ScheduleClientOptions.Builder) {

  def withNamespace(value: String): ZScheduleClientOptions =
    copy(namespace = Some(value))

  def withDataConverter(value: DataConverter): ZScheduleClientOptions =
    copy(dataConverter = Some(value))

  def withIdentity(value: String): ZScheduleClientOptions =
    copy(identity = Some(value))

  def withContextPropagators(value: ContextPropagator*): ZScheduleClientOptions =
    copy(contextPropagators = value.toList)

  /** Allows to specify options directly on the java SDK's [[ScheduleClientOptions]]. Use it in case an appropriate
    * `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: ScheduleClientOptions.Builder => ScheduleClientOptions.Builder
  ): ZScheduleClientOptions =
    copy(javaOptionsCustomization = f)

  def toJava: ScheduleClientOptions = {
    val builder = ScheduleClientOptions.newBuilder()
    namespace.foreach(builder.setNamespace)
    dataConverter.foreach(builder.setDataConverter)
    identity.foreach(builder.setIdentity)
    builder.setContextPropagators(contextPropagators.asJava)

    javaOptionsCustomization(builder).build()
  }
}

object ZScheduleClientOptions extends ConfigurationCompanion[ZScheduleClientOptions] {
  def withNamespace(value: String): Configure =
    configure(_.withNamespace(value))

  def withDataConverter(value: => DataConverter): Configure =
    configure(_.withDataConverter(value))

  def withIdentity(value: String): Configure =
    configure(_.withIdentity(value))

  def withContextPropagators(value: ContextPropagator*): Configure =
    configure(_.withContextPropagators(value: _*))

  def transformJavaOptions(
    f: ScheduleClientOptions.Builder => ScheduleClientOptions.Builder
  ): Configure = configure(_.transformJavaOptions(f))

  private val scheduleClientConfig =
    Config.string("namespace").optional ++
      Config.string("identity").optional

  /** Reads config from the default path `zio.temporal.zschedule_client`
    */
  val make: Layer[Config.Error, ZScheduleClientOptions] =
    makeImpl(Nil)

  /** Allows to specify custom path for the config
    */
  def forPath(name: String, names: String*): Layer[Config.Error, ZScheduleClientOptions] =
    makeImpl(List(name) ++ names)

  private def makeImpl(additionalPath: List[String]): Layer[Config.Error, ZScheduleClientOptions] = {
    val config = additionalPath match {
      case Nil          => scheduleClientConfig.nested("zio", "temporal", "zschedule_client")
      case head :: tail => scheduleClientConfig.nested(head, tail: _*)
    }

    ZLayer.fromZIO {
      ZIO.config(config).map { case (namespace, identityCfg) =>
        new ZScheduleClientOptions(
          namespace = namespace,
          dataConverter = None,
          identity = identityCfg,
          contextPropagators = Nil,
          javaOptionsCustomization = identity
        )
      }
    }
  }
}