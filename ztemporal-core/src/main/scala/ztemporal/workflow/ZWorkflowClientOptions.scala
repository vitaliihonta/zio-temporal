package ztemporal.workflow

import io.temporal.api.enums.v1.QueryRejectCondition
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.context.ContextPropagator
import io.temporal.common.converter.DataConverter
import io.temporal.common.interceptors.WorkflowClientInterceptor
import scala.jdk.CollectionConverters._

/** Represents temporal workflow client options
  *
  * @see
  *   [[WorkflowClientOptions]]
  */
class ZWorkflowClientOptions private[ztemporal] (
  val namespace:            Option[String],
  val dataConverter:        Option[DataConverter],
  val interceptors:         List[WorkflowClientInterceptor],
  val identity:             Option[String],
  val binaryChecksum:       Option[String],
  val contextPropagators:   List[ContextPropagator],
  val queryRejectCondition: Option[QueryRejectCondition]) {

  override def toString: String =
    s"ZWorkflowClientOptions(" +
      s"namespace=$namespace, " +
      s"dataConverter=$dataConverter, " +
      s"interceptors=$interceptors, " +
      s"identity=$identity, " +
      s"binaryChecksum=$binaryChecksum, " +
      s"contextPropagators=$contextPropagators, " +
      s"queryRejectCondition=$queryRejectCondition)"

  def withNamespace(value: String): ZWorkflowClientOptions =
    copy(_namespace = Some(value))

  def withDataConverter(value: DataConverter): ZWorkflowClientOptions =
    copy(_dataConverter = Some(value))

  def withInterceptors(value: WorkflowClientInterceptor*): ZWorkflowClientOptions =
    copy(_interceptors = value.toList)

  def withIdentity(value: String): ZWorkflowClientOptions =
    copy(_identity = Some(value))

  def withBinaryChecksum(value: String): ZWorkflowClientOptions =
    copy(_binaryChecksum = Some(value))

  def withContextPropagators(value: ContextPropagator*): ZWorkflowClientOptions =
    copy(_contextPropagators = value.toList)

  def withQueryRejectCondition(value: QueryRejectCondition): ZWorkflowClientOptions =
    copy(_queryRejectCondition = Some(value))

  def toJava: WorkflowClientOptions = {
    val builder = WorkflowClientOptions.newBuilder()

    namespace.foreach(builder.setNamespace)
    dataConverter.foreach(builder.setDataConverter)
    builder.setInterceptors(interceptors: _*)
    identity.foreach(builder.setIdentity)
    binaryChecksum.foreach(builder.setBinaryChecksum)
    builder.setContextPropagators(contextPropagators.asJava)
    queryRejectCondition.foreach(builder.setQueryRejectCondition)

    builder.build()
  }

  private def copy(
    _namespace:            Option[String] = namespace,
    _dataConverter:        Option[DataConverter] = dataConverter,
    _interceptors:         List[WorkflowClientInterceptor] = interceptors,
    _identity:             Option[String] = identity,
    _binaryChecksum:       Option[String] = binaryChecksum,
    _contextPropagators:   List[ContextPropagator] = contextPropagators,
    _queryRejectCondition: Option[QueryRejectCondition] = queryRejectCondition
  ): ZWorkflowClientOptions =
    new ZWorkflowClientOptions(
      _namespace,
      _dataConverter,
      _interceptors,
      _identity,
      _binaryChecksum,
      _contextPropagators,
      _queryRejectCondition
    )
}

object ZWorkflowClientOptions {

  val default: ZWorkflowClientOptions = new ZWorkflowClientOptions(
    namespace = None,
    dataConverter = None,
    interceptors = Nil,
    identity = None,
    binaryChecksum = None,
    contextPropagators = Nil,
    queryRejectCondition = None
  )
}
