package zio.temporal.workflow

import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext
import io.temporal.serviceclient.RpcRetryOptions
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import zio._

/** Represents temporal workflow service stubs options
  *
  * @see
  *   [[WorkflowServiceStubsOptions]]
  */
class ZWorkflowServiceStubsOptions private[zio] (
  val serverUrl:                       String,
  val channel:                         Option[ManagedChannel],
  val sslContext:                      Option[SslContext],
  val enableHttps:                     Option[Boolean],
  val enableKeepAlive:                 Option[Boolean],
  val keepAliveTime:                   Option[Duration],
  val keepAliveTimeout:                Option[Duration],
  val keepAlivePermitWithoutStream:    Option[Boolean],
  val rpcTimeout:                      Option[Duration],
  val rpcLongPollTimeout:              Option[Duration],
  val rpcQueryTimeout:                 Option[Duration],
  val rpcRetryOptions:                 Option[RpcRetryOptions],
  val connectionBackoffResetFrequency: Option[Duration],
  val grpcReconnectFrequency:          Option[Duration],
  val headers:                         Option[Metadata]) {

  def withChannel(value: ManagedChannel): ZWorkflowServiceStubsOptions =
    copy(_channel = Some(value))

  def withSslContext(value: SslContext): ZWorkflowServiceStubsOptions =
    copy(_sslContext = Some(value))

  def withEnableHttps(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(_enableHttps = Some(value))

  def withEnableKeepAlive(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(_enableKeepAlive = Some(value))

  def withKeepAliveTime(value: Duration): ZWorkflowServiceStubsOptions =
    copy(_keepAliveTime = Some(value))

  def withKeepAliveTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(_keepAliveTimeout = Some(value))

  def withKeepAlivePermitWithoutStream(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(_keepAlivePermitWithoutStream = Some(value))

  def withRpcTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(_rpcTimeout = Some(value))

  def withRpcLongPollTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(_rpcLongPollTimeout = Some(value))

  def withRpcQueryTimeout(value: Duration): ZWorkflowServiceStubsOptions =
    copy(_rpcQueryTimeout = Some(value))

  def withRpcRetryOptions(value: RpcRetryOptions): ZWorkflowServiceStubsOptions =
    copy(_rpcRetryOptions = Some(value))

  def withConnectionBackoffResetFrequency(value: Duration): ZWorkflowServiceStubsOptions =
    copy(_connectionBackoffResetFrequency = Some(value))

  def withGrpcReconnectFrequency(value: Duration): ZWorkflowServiceStubsOptions =
    copy(_grpcReconnectFrequency = Some(value))

  def withHeaders(value: Metadata): ZWorkflowServiceStubsOptions =
    copy(_headers = Some(value))

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
    builder.build()
  }

  private def copy(
    _serverUrl:                       String = serverUrl,
    _channel:                         Option[ManagedChannel] = channel,
    _sslContext:                      Option[SslContext] = sslContext,
    _enableHttps:                     Option[Boolean] = enableHttps,
    _enableKeepAlive:                 Option[Boolean] = enableKeepAlive,
    _keepAliveTime:                   Option[Duration] = keepAliveTime,
    _keepAliveTimeout:                Option[Duration] = keepAliveTimeout,
    _keepAlivePermitWithoutStream:    Option[Boolean] = keepAlivePermitWithoutStream,
    _rpcTimeout:                      Option[Duration] = rpcTimeout,
    _rpcLongPollTimeout:              Option[Duration] = rpcLongPollTimeout,
    _rpcQueryTimeout:                 Option[Duration] = rpcQueryTimeout,
    _rpcRetryOptions:                 Option[RpcRetryOptions] = rpcRetryOptions,
    _connectionBackoffResetFrequency: Option[Duration] = connectionBackoffResetFrequency,
    _grpcReconnectFrequency:          Option[Duration] = grpcReconnectFrequency,
    _headers:                         Option[Metadata] = headers
  ): ZWorkflowServiceStubsOptions =
    new ZWorkflowServiceStubsOptions(
      _serverUrl,
      _channel,
      _sslContext,
      _enableHttps,
      _enableKeepAlive,
      _keepAliveTime,
      _keepAliveTimeout,
      _keepAlivePermitWithoutStream,
      _rpcTimeout,
      _rpcLongPollTimeout,
      _rpcQueryTimeout,
      _rpcRetryOptions,
      _connectionBackoffResetFrequency,
      _grpcReconnectFrequency,
      _headers
    )
}

object ZWorkflowServiceStubsOptions {

  val DefaultLocalDocker: ZWorkflowServiceStubsOptions = new ZWorkflowServiceStubsOptions(
    serverUrl = "127.0.0.1:7233",
    channel = None,
    sslContext = None,
    enableHttps = None,
    enableKeepAlive = None,
    keepAliveTime = None,
    keepAliveTimeout = None,
    keepAlivePermitWithoutStream = None,
    rpcTimeout = None,
    rpcLongPollTimeout = None,
    rpcQueryTimeout = None,
    rpcRetryOptions = None,
    connectionBackoffResetFrequency = None,
    grpcReconnectFrequency = None,
    headers = None
  )
}
