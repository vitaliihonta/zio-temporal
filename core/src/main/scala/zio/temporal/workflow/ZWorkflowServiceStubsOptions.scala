package zio.temporal.workflow

import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext
import io.temporal.serviceclient.{GrpcMetadataProvider, RpcRetryOptions, WorkflowServiceStubsOptions}
import zio._
import zio.temporal.internal.ConfigurationCompanion

/** Represents temporal workflow service stubs options
  *
  * @see
  *   [[WorkflowServiceStubsOptions]]
  */
case class ZWorkflowServiceStubsOptions private[zio] (
  serverUrl:                            String,
  channel:                              Option[ManagedChannel],
  sslContext:                           Option[SslContext],
  enableHttps:                          Option[Boolean],
  enableKeepAlive:                      Option[Boolean],
  keepAliveTime:                        Option[Duration],
  keepAliveTimeout:                     Option[Duration],
  keepAlivePermitWithoutStream:         Option[Boolean],
  rpcTimeout:                           Option[Duration],
  rpcLongPollTimeout:                   Option[Duration],
  rpcQueryTimeout:                      Option[Duration],
  rpcRetryOptions:                      Option[RpcRetryOptions],
  connectionBackoffResetFrequency:      Option[Duration],
  grpcReconnectFrequency:               Option[Duration],
  headers:                              Option[Metadata],
  grpcMetadataProvider:                 Option[GrpcMetadataProvider],
  private val javaOptionsCustomization: WorkflowServiceStubsOptions.Builder => WorkflowServiceStubsOptions.Builder) {

  def withServiceUrl(value: String): ZWorkflowServiceStubsOptions =
    copy(serverUrl = value)

  def withChannel(value: ManagedChannel): ZWorkflowServiceStubsOptions =
    copy(channel = Some(value))

  def withSslContext(value: SslContext): ZWorkflowServiceStubsOptions =
    copy(sslContext = Some(value))

  def withEnableHttps(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(enableHttps = Some(value))

  def withEnableKeepAlive(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(enableKeepAlive = Some(value))

  def withKeepAliveTime(value: Duration): ZWorkflowServiceStubsOptions =
    copy(keepAliveTime = Some(value))

  def withKeepAliveTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(keepAliveTimeout = Some(value))

  def withKeepAlivePermitWithoutStream(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(keepAlivePermitWithoutStream = Some(value))

  def withRpcTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(rpcTimeout = Some(value))

  def withRpcLongPollTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(rpcLongPollTimeout = Some(value))

  def withRpcQueryTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(rpcQueryTimeout = Some(value))

  def withRpcRetryOptions(value: RpcRetryOptions): ZWorkflowServiceStubsOptions =
    copy(rpcRetryOptions = Some(value))

  def withConnectionBackoffResetFrequency(value: Duration): ZWorkflowServiceStubsOptions =
    copy(connectionBackoffResetFrequency = Some(value))

  def withGrpcReconnectFrequency(value: Duration): ZWorkflowServiceStubsOptions =
    copy(grpcReconnectFrequency = Some(value))

  def withHeaders(value: Metadata): ZWorkflowServiceStubsOptions =
    copy(headers = Some(value))

  /** @param grpcMetadataProvider
    *   gRPC metadata/headers provider to be called on each gRPC request to supply additional headers
    */
  def withGrpcMetadataProvider(grpcMetadataProvider: GrpcMetadataProvider): ZWorkflowServiceStubsOptions =
    copy(grpcMetadataProvider = Some(grpcMetadataProvider))

  /** Allows to specify options directly on the java SDK's [[WorkflowServiceStubsOptions]]. Use it in case an
    * appropriate `withXXX` method is missing
    *
    * @note
    *   the options specified via this method take precedence over those specified via other methods.
    */
  def transformJavaOptions(
    f: WorkflowServiceStubsOptions.Builder => WorkflowServiceStubsOptions.Builder
  ): ZWorkflowServiceStubsOptions =
    copy(javaOptionsCustomization = f)

  def toJava: WorkflowServiceStubsOptions = {
    val builder = WorkflowServiceStubsOptions.newBuilder()

    builder.setTarget(serverUrl)
    channel.foreach(builder.setChannel)
    sslContext.foreach(builder.setSslContext)
    enableHttps.foreach(builder.setEnableHttps)
    enableKeepAlive.foreach(builder.setEnableKeepAlive)
    keepAliveTime.foreach(t => builder.setKeepAliveTime(t.asJava))
    keepAliveTimeout.foreach(t => builder.setKeepAliveTimeout(t.asJava))
    keepAlivePermitWithoutStream.foreach(builder.setKeepAlivePermitWithoutStream)
    rpcTimeout.foreach(t => builder.setRpcTimeout(t.asJava))
    rpcLongPollTimeout.foreach(t => builder.setRpcLongPollTimeout(t.asJava))
    rpcQueryTimeout.foreach(t => builder.setRpcQueryTimeout(t.asJava))
    rpcRetryOptions.foreach(builder.setRpcRetryOptions)
    connectionBackoffResetFrequency.foreach(t => builder.setConnectionBackoffResetFrequency(t.asJava))
    grpcReconnectFrequency.foreach(t => builder.setGrpcReconnectFrequency(t.asJava))
    headers.foreach(builder.setHeaders)
    grpcMetadataProvider.foreach(builder.addGrpcMetadataProvider)

    javaOptionsCustomization(builder).build()
  }
}

object ZWorkflowServiceStubsOptions extends ConfigurationCompanion[ZWorkflowServiceStubsOptions] {

  def withServiceUrl(value: String): Configure =
    configure(_.withServiceUrl(value))

  def withChannel(value: ManagedChannel): Configure =
    configure(_.withChannel(value))

  def withSslContext(value: SslContext): Configure =
    configure(_.withSslContext(value))

  def withEnableHttps(value: Boolean): Configure =
    configure(_.withEnableHttps(value))

  def withEnableKeepAlive(value: Boolean): Configure =
    configure(_.withEnableKeepAlive(value))

  def withKeepAliveTime(value: Duration): Configure =
    configure(_.withKeepAliveTime(value))

  def withKeepAliveTimeout(value: Duration): Configure =
    configure(_.withKeepAliveTimeout(value))

  def withKeepAlivePermitWithoutStream(value: Boolean): Configure =
    configure(_.withKeepAlivePermitWithoutStream(value))

  def withRpcTimeout(value: Duration): Configure =
    configure(_.withRpcTimeout(value))

  def withRpcLongPollTimeout(value: Duration): Configure =
    configure(_.withRpcLongPollTimeout(value))

  def withRpcQueryTimeout(value: Duration): Configure =
    configure(_.withRpcQueryTimeout(value))

  def withRpcRetryOptions(value: RpcRetryOptions): Configure =
    configure(_.withRpcRetryOptions(value))

  def withConnectionBackoffResetFrequency(value: Duration): Configure =
    configure(_.withConnectionBackoffResetFrequency(value))

  def withGrpcReconnectFrequency(value: Duration): Configure =
    configure(_.withGrpcReconnectFrequency(value))

  def withHeaders(value: Metadata): Configure =
    configure(_.withHeaders(value))

  def withGrpcMetadataProvider(grpcMetadataProvider: GrpcMetadataProvider): Configure =
    configure(_.withGrpcMetadataProvider(grpcMetadataProvider))

  def transformJavaOptions(
    f: WorkflowServiceStubsOptions.Builder => WorkflowServiceStubsOptions.Builder
  ): Configure =
    configure(_.transformJavaOptions(f))

  private val workflowServiceStubsConfig =
    Config.string("server_url").withDefault("127.0.0.1:7233") ++
      Config.boolean("enable_https").optional ++
      Config.boolean("enable_keep_alive").optional ++
      Config.duration("keep_alive_time").optional ++
      Config.duration("keep_alive_timeout").optional ++
      Config.boolean("keep_alive_permit_without_stream").optional ++
      Config.duration("rpc_timeout").optional ++
      Config.duration("rpc_long_poll_timeout").optional ++
      Config.duration("rpc_query_timeout").optional ++
      Config.duration("connection_backoff_reset_frequency").optional ++
      Config.duration("grpc_reconnect_frequency").optional

  /** Reads config from the default path `zio.temporal.ZWorkflowServiceStubs`
    */
  val make: Layer[Config.Error, ZWorkflowServiceStubsOptions] =
    makeImpl(Nil)

  /** Allows to specify custom path for the config
    */
  def forPath(name: String, names: String*): Layer[Config.Error, ZWorkflowServiceStubsOptions] =
    makeImpl(List(name) ++ names)

  private def makeImpl(additionalPath: List[String]): Layer[Config.Error, ZWorkflowServiceStubsOptions] = {
    val config = additionalPath match {
      case Nil          => workflowServiceStubsConfig.nested("zio", "temporal", "zworkflow_service_stubs")
      case head :: tail => workflowServiceStubsConfig.nested(head, tail: _*)
    }
    ZLayer.fromZIO {
      ZIO.config(config).map {
        case (
              serverUrl,
              enableHttps,
              enableKeepAlive,
              keepAliveTime,
              keepAliveTimeout,
              keepAlivePermitWithoutStream,
              rpcTimeout,
              rpcLongPollTimeout,
              rpcQueryTimeout,
              connectionBackoffResetFrequency,
              grpcReconnectFrequency
            ) =>
          new ZWorkflowServiceStubsOptions(
            serverUrl = serverUrl,
            channel = None,
            sslContext = None,
            enableHttps = enableHttps,
            enableKeepAlive = enableKeepAlive,
            keepAliveTime = keepAliveTime,
            keepAliveTimeout = keepAliveTimeout,
            keepAlivePermitWithoutStream = keepAlivePermitWithoutStream,
            rpcTimeout = rpcTimeout,
            rpcLongPollTimeout = rpcLongPollTimeout,
            rpcQueryTimeout = rpcQueryTimeout,
            rpcRetryOptions = None,
            connectionBackoffResetFrequency = connectionBackoffResetFrequency,
            grpcReconnectFrequency = grpcReconnectFrequency,
            headers = None,
            grpcMetadataProvider = None,
            javaOptionsCustomization = identity
          )
      }
    }
  }
}
