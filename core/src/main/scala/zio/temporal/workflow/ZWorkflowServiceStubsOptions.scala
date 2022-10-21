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
case class ZWorkflowServiceStubsOptions private[zio] (
  serverUrl:                       String,
  channel:                         Option[ManagedChannel],
  sslContext:                      Option[SslContext],
  enableHttps:                     Option[Boolean],
  enableKeepAlive:                 Option[Boolean],
  keepAliveTime:                   Option[Duration],
  keepAliveTimeout:                Option[Duration],
  keepAlivePermitWithoutStream:    Option[Boolean],
  rpcTimeout:                      Option[Duration],
  rpcLongPollTimeout:              Option[Duration],
  rpcQueryTimeout:                 Option[Duration],
  rpcRetryOptions:                 Option[RpcRetryOptions],
  connectionBackoffResetFrequency: Option[Duration],
  grpcReconnectFrequency:          Option[Duration],
  headers:                         Option[Metadata]) {

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
}

object ZWorkflowServiceStubsOptions {

  val default: ZWorkflowServiceStubsOptions = new ZWorkflowServiceStubsOptions(
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
