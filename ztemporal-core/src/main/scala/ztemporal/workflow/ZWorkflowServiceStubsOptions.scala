package ztemporal.workflow

import io.grpc.ManagedChannel
import io.grpc.Metadata
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext
import io.temporal.serviceclient.RpcRetryOptions
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import scala.concurrent.duration.FiniteDuration
import scala.compat.java8.DurationConverters._

/** Represents temporal workflow service stubs options
  *
  *  @see [[WorkflowServiceStubsOptions]]
  */
class ZWorkflowServiceStubsOptions private[ztemporal] (
  val serverUrl:                       String,
  val channel:                         Option[ManagedChannel],
  val sslContext:                      Option[SslContext],
  val enableHttps:                     Option[Boolean],
  val enableKeepAlive:                 Option[Boolean],
  val keepAliveTime:                   Option[FiniteDuration],
  val keepAliveTimeout:                Option[FiniteDuration],
  val keepAlivePermitWithoutStream:    Option[Boolean],
  val rpcTimeout:                      Option[FiniteDuration],
  val rpcLongPollTimeout:              Option[FiniteDuration],
  val rpcQueryTimeout:                 Option[FiniteDuration],
  val rpcRetryOptions:                 Option[RpcRetryOptions],
  val connectionBackoffResetFrequency: Option[FiniteDuration],
  val grpcReconnectFrequency:          Option[FiniteDuration],
  val headers:                         Option[Metadata]) {

  def withChannel(value: ManagedChannel): ZWorkflowServiceStubsOptions =
    copy(_channel = Some(value))

  def withSslContext(value: SslContext): ZWorkflowServiceStubsOptions =
    copy(_sslContext = Some(value))

  def withEnableHttps(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(_enableHttps = Some(value))

  def withEnableKeepAlive(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(_enableKeepAlive = Some(value))

  def withKeepAliveTime(value: FiniteDuration): ZWorkflowServiceStubsOptions =
    copy(_keepAliveTime = Some(value))

  def withKeepAliveTimeout(value: FiniteDuration): ZWorkflowServiceStubsOptions =
    copy(_keepAliveTimeout = Some(value))

  def withKeepAlivePermitWithoutStream(value: Boolean): ZWorkflowServiceStubsOptions =
    copy(_keepAlivePermitWithoutStream = Some(value))

  def withRpcTimeout(value: FiniteDuration): ZWorkflowServiceStubsOptions =
    copy(_rpcTimeout = Some(value))

  def withRpcLongPollTimeout(value: FiniteDuration): ZWorkflowServiceStubsOptions =
    copy(_rpcLongPollTimeout = Some(value))

  def withRpcQueryTimeout(value: FiniteDuration): ZWorkflowServiceStubsOptions =
    copy(_rpcQueryTimeout = Some(value))

  def withRpcRetryOptions(value: RpcRetryOptions): ZWorkflowServiceStubsOptions =
    copy(_rpcRetryOptions = Some(value))

  def withConnectionBackoffResetFrequency(value: FiniteDuration): ZWorkflowServiceStubsOptions =
    copy(_connectionBackoffResetFrequency = Some(value))

  def withGrpcReconnectFrequency(value: FiniteDuration): ZWorkflowServiceStubsOptions =
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
    keepAliveTime.foreach(t => builder.setKeepAliveTime(t.toJava))
    keepAliveTimeout.foreach(t => builder.setKeepAliveTimeout(t.toJava))
    keepAlivePermitWithoutStream.foreach(builder.setKeepAlivePermitWithoutStream)
    rpcTimeout.foreach(t => builder.setRpcTimeout(t.toJava))
    rpcLongPollTimeout.foreach(t => builder.setRpcLongPollTimeout(t.toJava))
    rpcQueryTimeout.foreach(t => builder.setRpcQueryTimeout(t.toJava))
    rpcRetryOptions.foreach(builder.setRpcRetryOptions)
    connectionBackoffResetFrequency.foreach(t => builder.setConnectionBackoffResetFrequency(t.toJava))
    grpcReconnectFrequency.foreach(t => builder.setGrpcReconnectFrequency(t.toJava))
    headers.foreach(builder.setHeaders)
    builder.build()
  }

  private def copy(
    _serverUrl:                       String = serverUrl,
    _channel:                         Option[ManagedChannel] = channel,
    _sslContext:                      Option[SslContext] = sslContext,
    _enableHttps:                     Option[Boolean] = enableHttps,
    _enableKeepAlive:                 Option[Boolean] = enableKeepAlive,
    _keepAliveTime:                   Option[FiniteDuration] = keepAliveTime,
    _keepAliveTimeout:                Option[FiniteDuration] = keepAliveTimeout,
    _keepAlivePermitWithoutStream:    Option[Boolean] = keepAlivePermitWithoutStream,
    _rpcTimeout:                      Option[FiniteDuration] = rpcTimeout,
    _rpcLongPollTimeout:              Option[FiniteDuration] = rpcLongPollTimeout,
    _rpcQueryTimeout:                 Option[FiniteDuration] = rpcQueryTimeout,
    _rpcRetryOptions:                 Option[RpcRetryOptions] = rpcRetryOptions,
    _connectionBackoffResetFrequency: Option[FiniteDuration] = connectionBackoffResetFrequency,
    _grpcReconnectFrequency:          Option[FiniteDuration] = grpcReconnectFrequency,
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
