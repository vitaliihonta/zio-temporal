package ztemporal.distage

import izumi.distage.config.ConfigModuleDef
import izumi.distage.model.definition.ModuleDef
import pureconfig.ConfigReader
import pureconfig.module.magnolia.semiauto.reader.deriveReader
import ztemporal.activity.ZActivityOptions
import ztemporal.worker.ZWorkerFactory
import ztemporal.workflow.ZWorkflowClient
import ztemporal.workflow.ZWorkflowClientOptions
import ztemporal.workflow.ZWorkflowServiceStubs
import ztemporal.workflow.ZWorkflowServiceStubsOptions
import scala.concurrent.duration.FiniteDuration

/** A module containing parsed [[ZWorkflowServiceStubsOptions]]
  */
object ZTemporalConfigModule extends ConfigModuleDef {

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
  makeConfig[ZWorkflowServiceStubsReadableConfig]("ztemporal")

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
      headers = None,
      blockingStubInterceptor = None,
      futureStubInterceptor = None
    )
  }
}

/** A base module containing parsed everything needed for temporal worker or client
  */
object ZBaseTemporalModule extends ModuleDef {
  make[ZWorkflowServiceStubs].fromResource(ZWorkflowServiceStubs.make(_: ZWorkflowServiceStubsOptions))
  make[ZWorkflowClient].from(ZWorkflowClient.make(_: ZWorkflowServiceStubs, _: ZWorkflowClientOptions))
}

/** A module for temporal client service
  */
object ZTemporalClientModule extends ModuleDef {
  include(ZBaseTemporalModule)
}

/** A module for temporal worker
  */
object ZTemporalWorkerModule extends ModuleDef {
  include(ZBaseTemporalModule)

  make[ZWorkerFactory].fromEffect(ZWorkerFactory.make(_: ZWorkflowClient))
  make[ZActivityOptions].fromEffect(ZActivityOptions.make(_: ZWorkflowClient))

  make[ZWorkerLifecycle]
}
