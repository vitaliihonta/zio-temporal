package zio.temporal.func

import io.temporal.client.WorkflowClient
import io.temporal.workflow.Async
import io.temporal.workflow.Functions._
import zio.temporal.{TemporalClientError, TemporalError, TemporalIO, ZWorkflowExecution}
import zio.temporal.internal.TemporalInteraction
import zio.temporal.promise.ZPromise

class ZFunc0[A](private val self: () => A) extends AnyVal {

  /** Executes zero argument workflow waiting for it's result.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @return
    *   IO that contains workflow result or failure
    */
  def execute: TemporalIO[TemporalClientError, A] =
    TemporalInteraction.fromFuture {
      WorkflowClient.execute((() => self()): Func[A])
    }

  /** Executes zero argument workflow waiting for it's result.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @return
    *   IO that contains workflow result or failure
    */
  def executeEither[E, V](implicit ev: A <:< Either[E, V]): TemporalIO[TemporalError[E], V] =
    TemporalInteraction.fromFutureEither {
      WorkflowClient.execute((() => ev(self())): Func[Either[E, V]])
    }

  /** Starts zero argument workflow.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @return
    *   IO that contains workflow execution info
    */
  def start: TemporalIO[TemporalClientError, ZWorkflowExecution] =
    TemporalInteraction.from {
      new ZWorkflowExecution(
        WorkflowClient.start((() => self()): Func[A])
      )
    }

  /** Executes zero argument workflow in background.
    *
    * @return
    *   [[ZPromise]] that contains workflow result or failure
    */
  def async: ZPromise[Nothing, A] =
    ZPromise.fromEither(Right(self()))

  /** Executes zero argument workflow in background.
    *
    * @return
    *   [[ZPromise]] that contains workflow result or failure
    */
  def asyncEither[E, V](implicit ev: A <:< Either[E, V]): ZPromise[E, V] =
    new ZPromise.Impl[E, V](Async.function(() => ev(self())))

}

class ZFunc1[A, B](private val self: A => B) extends AnyVal {

  /** Executes one argument workflow waiting for it's result.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @param a
    *   workflow argument
    * @return
    *   IO that contains workflow result or failure
    */
  def execute(a: A): TemporalIO[TemporalClientError, B] =
    TemporalInteraction.fromFuture {
      WorkflowClient.execute(self(_: A), a)
    }

  /** Executes one argument workflow waiting for it's result.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @param a
    *   workflow argument
    * @return
    *   IO that contains workflow result or failure
    */
  def executeEither[E, V](a: A)(implicit ev: B <:< Either[E, V]): TemporalIO[TemporalError[E], V] =
    TemporalInteraction.fromFutureEither {
      WorkflowClient.execute(((a: A) => ev(self(a))): Func1[A, Either[E, V]], a)
    }

  /** Starts one argument workflow.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @param a
    *   workflow argument
    * @return
    *   IO that contains workflow execution info
    */
  def start(a: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    TemporalInteraction.from {
      new ZWorkflowExecution(
        WorkflowClient.start((self(_)): Func1[A, B], a)
      )
    }

  /** Executes one argument workflow in background.
    *
    * @param a
    *   workflow argument
    * @return
    *   [[ZPromise]] that contains workflow result or failure
    */
  def async(a: A): ZPromise[Nothing, B] =
    new ZPromise.Impl[Nothing, B](Async.function((a: A) => Right(self(a)), a))

  /** Executes one argument workflow in background.
    *
    * @param a
    *   workflow argument
    * @return
    *   [[ZPromise]] that contains workflow result or failure
    */
  def asyncEither[E, V](a: A)(implicit ev: B <:< Either[E, V]): ZPromise[E, V] =
    new ZPromise.Impl[E, V](Async.function((a: A) => ev(self(a)), a))

}

class ZFunc2[A, B, C](private val self: (A, B) => C) extends AnyVal {

  /** Executes two argument workflow waiting for it's result.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @param a
    *   first workflow argument
    * @param b
    *   second workflow argument
    * @return
    *   IO that contains workflow result or failure
    */
  def execute(a: A, b: B): TemporalIO[TemporalClientError, C] =
    TemporalInteraction.fromFuture {
      WorkflowClient.execute(self(_: A, _: B), a, b)
    }

  /** Executes two argument workflow waiting for it's result.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @param a
    *   first workflow argument
    * @param b
    *   second workflow argument
    * @return
    *   IO that contains workflow result or failure
    */
  def executeEither[E, V](a: A, b: B)(implicit ev: C <:< Either[E, V]): TemporalIO[TemporalError[E], V] =
    TemporalInteraction.fromFutureEither {
      WorkflowClient.execute(((a: A, b: B) => ev(self(a, b))): Func2[A, B, Either[E, V]], a, b)
    }

  /** Starts two argument workflow.
    *
    * The only supported value is method reference to a proxy created through
    * [[zio.temporal.workflow.ZWorkflowClient.newWorkflowStub]]
    * @param a
    *   first workflow argument
    * @param b
    *   second workflow argument
    * @return
    *   IO that contains workflow execution info
    */
  def start(a: A, b: B): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    TemporalInteraction.from {
      new ZWorkflowExecution(
        WorkflowClient.start(self(_: A, _: B), a, b)
      )
    }

  /** Executes two argument workflow in background.
    *
    * @param a
    *   first workflow argument
    * @param b
    *   second workflow argument
    * @return
    *   [[ZPromise]] that contains workflow result or failure
    */
  def async(a: A, b: B): ZPromise[Nothing, C] =
    new ZPromise.Impl[Nothing, C](Async.function((a: A, b: B) => Right(self(a, b)), a, b))

  /** Executes zero argument workflow in background.
    *
    * @param a
    *   first workflow argument
    * @param b
    *   second workflow argument
    * @return
    *   [[ZPromise]] that contains workflow result or failure
    */
  def asyncEither[E, V](a: A, b: B)(implicit ev: C <:< Either[E, V]): ZPromise[E, V] =
    new ZPromise.Impl[E, V](Async.function((a: A, b: B) => ev(self(a, b)), a, b))

}
