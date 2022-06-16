package ztemporal.func

import ztemporal.ZTemporalClientError
import ztemporal.ZTemporalError
import ztemporal.ZTemporalIO
import ztemporal.internalApi
import ztemporal.internal.TemporalInteraction
import ztemporal.workflow.ZWorkflowStub

/** Represents workflow query (method annotated with [[io.temporal.workflow.QueryMethod]])
  *
  * @see
  *   [[io.temporal.client.WorkflowClientOptions.Builder#setQueryRejectCondition(QueryRejectCondition)]]
  * @tparam R
  *   query result
  * @param queryType
  *   query method name (either taken from annotation or just scala method name)
  */
class ZWorkflowQuery0[R] @internalApi() (stub: ZWorkflowStub, cls: Class[R], queryType: String) {

  /** Queries workflow by invoking its query handler.
    * @return
    *   query result or error
    */
  def run: ZTemporalIO[ZTemporalClientError, R] =
    TemporalInteraction.from {
      stub.self.query[R](queryType, cls)
    }

  /** Queries workflow by invoking its query handle.
    * @return
    *   query result or error
    */
  def runEither[E, V](implicit ev: R <:< Either[E, V]): ZTemporalIO[ZTemporalError[E], V] =
    TemporalInteraction.fromEither {
      ev(stub.self.query[R](queryType, cls))
    }
}

/** Represents workflow query (method annotated with [[io.temporal.workflow.QueryMethod]])
  *
  * @see
  *   [[io.temporal.client.WorkflowClientOptions.Builder#setQueryRejectCondition(QueryRejectCondition)]]
  * @tparam R
  *   query result
  * @tparam A
  *   query method input parameter
  * @param queryType
  *   query method name (either taken from annotation or just scala method name)
  */
class ZWorkflowQuery1[A, R] @internalApi() (stub: ZWorkflowStub, cls: Class[R], queryType: String) {

  /** Queries workflow by invoking its query handler.
    *
    * @param a
    *   query parameter
    * @return
    *   query result or error
    */
  def run(a: A): ZTemporalIO[ZTemporalClientError, R] =
    TemporalInteraction.from {
      stub.self.query[R](queryType, cls, a.asInstanceOf[AnyRef])
    }

  /** Queries workflow by invoking its query handler.
    *
    * @param a
    *   query parameter
    * @return
    *   query result or error
    */
  def runEither[E, V](a: A)(implicit ev: R <:< Either[E, V]): ZTemporalIO[ZTemporalError[E], V] =
    TemporalInteraction.fromEither {
      ev(stub.self.query[R](queryType, cls, a.asInstanceOf[AnyRef]))
    }
}

/** Represents workflow query (method annotated with [[io.temporal.workflow.QueryMethod]])
  *
  * @see
  *   [[io.temporal.client.WorkflowClientOptions.Builder#setQueryRejectCondition(QueryRejectCondition)]]
  * @tparam R
  *   query result
  * @tparam A
  *   the first query method input parameter
  * @tparam B
  *   the second query method input parameter
  * @param queryType
  *   query method name (either taken from annotation or just scala method name)
  */
class ZWorkflowQuery2[A, B, R] @internalApi() (stub: ZWorkflowStub, cls: Class[R], queryType: String) {

  /** Queries workflow by invoking its query handler.
    *
    * @param a
    *   first query parameter
    * @param b
    *   second query parameter
    * @return
    *   query result or error
    */
  def run(a: A, b: B): ZTemporalIO[ZTemporalClientError, R] =
    TemporalInteraction.from {
      stub.self.query[R](queryType, cls, a.asInstanceOf[AnyRef], b.asInstanceOf[AnyRef])
    }

  /** Queries workflow by invoking its query handler.
    *
    * @param a
    *   first query parameter
    * @param b
    *   second query parameter
    * @return
    *   query result or error
    */
  def runEither[E, V](a: A, b: B)(implicit ev: R <:< Either[E, V]): ZTemporalIO[ZTemporalError[E], V] =
    TemporalInteraction.fromEither {
      ev(stub.self.query[R](queryType, cls, a.asInstanceOf[AnyRef], b.asInstanceOf[AnyRef]))
    }
}
