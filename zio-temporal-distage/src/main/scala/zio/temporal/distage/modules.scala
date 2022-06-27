package zio.temporal.distage

import izumi.distage.config.ConfigModuleDef
import izumi.distage.model.definition.ModuleDef
import pureconfig.ConfigReader
import pureconfig.module.magnolia.semiauto.reader.deriveReader
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker.ZWorkerFactory
import zio.temporal.workflow.ZWorkflowClient
import zio.temporal.workflow.ZWorkflowClientOptions
import zio.temporal.workflow.ZWorkflowServiceStubs
import zio.temporal.workflow.ZWorkflowServiceStubsOptions

import scala.concurrent.duration.FiniteDuration

/** A module containing parsed [[ZWorkflowServiceStubsOptions]]
  */
object TemporalZioConfigModule extends ConfigModuleDef {

  /** Helper case class with a subset of parsable [[ZWorkflowServiceStubsOptions]] parameters
    */
  final case class ZWorkflowServiceStubsReadableConfig(
    serverUrl:                       String,
    enableHttps:                     Option[Boolean],
    enableKeepAlive:                 Option[Boolean],
    keepAliveTime:                   Option[FiniteDuration],
    keepAliveTimeout:                Option[FiniteDuration],
    keepAlivePermitWithoutStream:    Option[Boolean],
    rpcTimeout:                      Option[FiniteDuration],
    rpcLongPollTimeout:              Option[FiniteDuration],
    rpcQueryTimeout:                 Option[FiniteDuration],
    connectionBackoffResetFrequency: Option[FiniteDuration],
    grpcReconnectFrequency:          Option[FiniteDuration])

  protected implicit val zWorkflowServiceStubsOptionsConfigReader: ConfigReader[ZWorkflowServiceStubsReadableConfig] =
    deriveReader[ZWorkflowServiceStubsReadableConfig]

  /** Not recommended to be modified */
  makeConfig[ZWorkflowServiceStubsReadableConfig]("zio.temporal")

  /** Can be customized using [[modify]] DSL */
  make[ZWorkflowServiceStubsOptions].from { (config: ZWorkflowServiceStubsReadableConfig) =>
    new ZWorkflowServiceStubsOptions(
      serverUrl = config.serverUrl,
      channel = None,
      sslContext = None,
      enableHttps = config.enableHttps,
      enableKeepAlive = config.enableKeepAlive,
      keepAliveTime = config.keepAliveTime,
      keepAliveTimeout = config.keepAliveTimeout,
      keepAlivePermitWithoutStream = config.keepAlivePermitWithoutStream,
      rpcTimeout = config.rpcTimeout,
      rpcLongPollTimeout = config.rpcLongPollTimeout,
      rpcQueryTimeout = config.rpcQueryTimeout,
      rpcRetryOptions = None,
      connectionBackoffResetFrequency = config.connectionBackoffResetFrequency,
      grpcReconnectFrequency = config.grpcReconnectFrequency,
      headers = None
    )
  }
}

/** A base module containing parsed everything needed for temporal worker or client
  */
object BaseTemporalZioModule extends ModuleDef {
  make[ZWorkflowServiceStubs].fromResource(ZWorkflowServiceStubs.make(_: ZWorkflowServiceStubsOptions))
  make[ZWorkflowClient].from(ZWorkflowClient.make(_: ZWorkflowServiceStubs, _: ZWorkflowClientOptions))
}

/** A module for temporal client service
  */
object TemporalZioClientModule extends ModuleDef {
  include(BaseTemporalZioModule)
}

/** A module for temporal worker
  */
object TemporalZioWorkerModule extends ModuleDef {
  include(BaseTemporalZioModule)

  make[ZWorkerFactory].fromEffect(ZWorkerFactory.make(_: ZWorkflowClient))
  make[ZActivityOptions].fromEffect(ZActivityOptions.make(_: ZWorkflowClient))

  make[ZWorkerLifecycle]
}
