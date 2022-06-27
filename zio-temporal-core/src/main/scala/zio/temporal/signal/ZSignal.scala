package zio.temporal.signal

import io.temporal.client.BatchRequest
import zio.temporal.internal.ZSignalMacro
import zio.temporal.internalApi

import scala.annotation.implicitNotFound
import scala.language.experimental.macros

/** Represents a call to a signal handler method.
  *
  * @see
  *   [[io.temporal.workflow.SignalMethod]]
  * @tparam A
  *   signal method input
  * @tparam T
  *   signal method type
  */
class ZSignal[-A, T <: ZSignal.Type] @internalApi() (
  private[zio] val tpe:         T,
  private[zio] val addRequests: (ZInput[_], BatchRequest) => Unit)
    extends Serializable {

  /** Provide this signal input in-place.
    *
    * @param value
    *   this signal input parameter
    * @return
    *   this signal without inputs
    */
  def provide[B <: A](value: B)(implicit inputFrom: ZInput.From[B]): ZSignal[Any, T] =
    new ZSignal[Any, T](
      tpe,
      (_, batchRequest) => {
        val newInput = inputFrom(value)
        addRequests(newInput, batchRequest)
      }
    )

  /** Combines this ZSignal with the specified one.
    */
  def ++[B, N2 <: ZSignal.Type](
    that:               ZSignal[B, N2]
  )(implicit canConcat: ZSignal.CanConcat[T, N2]
  ): ZSignal[A with B, canConcat.Out] =
    new ZSignal[A with B, canConcat.Out](
      canConcat(tpe, that.tpe),
      (input, batchRequest) => {
        addRequests(input, batchRequest)
        that.addRequests(input, batchRequest)
      }
    )
}

object ZSignal {

  /** Represents signal type, can be one of:
    *   - [[SignalMethod]] - a single call to the signal method
    *   - [[WorkflowMethod]] - a call to workflow handler method (uncomplete signal)
    *   - [[SignalWithStart]] - a batch request calling workflow method and then signal method
    * @see
    *   [[BatchRequest]]
    */
  sealed trait Type
  sealed trait WorkflowMethod                                              extends Type
  case object WorkflowMethod                                               extends WorkflowMethod
  class SignalMethod @internalApi() (private[zio] val signalName: String)  extends Type
  class SignalWithStart private[zio] (private[zio] val signalName: String) extends Type
  // todo: add class Multiple() extends Type to send few signals at once

  /** Creates [[ZSignal]] from the specified signal handler method.
    *
    * This method won't compile if the passed function is not a method [[io.temporal.workflow.SignalMethod]] which
    * belongs to workflow interface
    *
    * @param f
    *   signal method call
    * @return
    *   a new ZSignal
    */
  def signal0(f: Unit): ZSignal[Any, ZSignal.SignalMethod] =
    macro ZSignalMacro.signalImpl0

  /** Creates [[ZSignal]] from the specified signal handler method.
    *
    * This method won't compile if the passed function is not a method [[io.temporal.workflow.SignalMethod]] which
    * belongs to workflow interface
    *
    * @tparam A
    *   signal method input parameter
    * @param f
    *   signal method call
    * @return
    *   a new ZSignal
    */
  def signal[A](f: A => Unit): ZSignal[ZInput[A], ZSignal.SignalMethod] =
    macro ZSignalMacro.signalImpl1[A]

  /** Creates [[ZSignal]] from the specified signal handler method.
    *
    * This method won't compile if the passed function is not a method [[io.temporal.workflow.SignalMethod]] which
    * belongs to workflow interface
    *
    * @tparam A
    *   first signal method input parameter
    * @tparam B
    *   second signal method input parameter
    * @param f
    *   signal method call
    * @return
    *   a new ZSignal
    */
  def signal[A, B](f: (A, B) => Unit): ZSignal[ZInput[A] with ZInput[B], ZSignal.SignalMethod] =
    macro ZSignalMacro.signalImpl2[A, B]

  /** Creates [[ZSignal]] from the specified workflow handler method.
    *
    * This method won't compile if the passed function is not a method [[io.temporal.workflow.WorkflowMethod]] which
    * belongs to workflow interface
    *
    * @tparam R
    *   workflow method result
    * @param f
    *   workflow method call
    * @return
    *   a new ZSignal
    */
  def workflowMethod0[R](f: R): ZSignal[Any, ZSignal.WorkflowMethod] =
    macro ZSignalMacro.workflowMethodImpl0[R]

  /** Creates [[ZSignal]] from the specified workflow handler method.
    *
    * This method won't compile if the passed function is not a method [[io.temporal.workflow.WorkflowMethod]] which
    * belongs to workflow interface
    *
    * @tparam A
    *   workflow method input parameter
    * @tparam R
    *   workflow method result
    * @param f
    *   workflow method call
    * @return
    *   a new ZSignal
    */
  def workflowMethod[A, R](f: A => R): ZSignal[ZInput[A], ZSignal.WorkflowMethod] =
    macro ZSignalMacro.workflowMethodImpl1[A, R]

  /** Creates [[ZSignal]] from the specified workflow handler method.
    *
    * This method won't compile if the passed function is not a method [[io.temporal.workflow.WorkflowMethod]] which
    * belongs to workflow interface
    *
    * @tparam A
    *   first workflow method input parameter
    * @tparam B
    *   second workflow method input parameter
    * @tparam R
    *   workflow method result
    * @param f
    *   workflow method call
    * @return
    *   a new ZSignal
    */
  def workflowMethod0[A, B, R](f: (A, B) => R): ZSignal[ZInput[A] with ZInput[B], ZSignal.WorkflowMethod] =
    macro ZSignalMacro.workflowMethodImpl2[A, B, R]

  @implicitNotFound("""Cannot concat ZSignals ${T1} and ${T2}.
You should consider concatenating only ZSignal of WorkflowMethod with ZSignal of SignalMethod
representing a BatchRequest""")
  sealed trait CanConcat[T1 <: Type, T2 <: Type] {
    type Out <: Type

    def apply(x: T1, y: T2): Out
  }

  object CanConcat {
    type Of[T1 <: Type, T2 <: Type, TOut <: Type] = CanConcat[T1, T2] { type Out = TOut }

    private def create[T1 <: Type, T2 <: Type, TOut <: Type](f: (T1, T2) => TOut): CanConcat.Of[T1, T2, TOut] =
      new CanConcat[T1, T2] {
        override type Out = TOut
        override def apply(x: T1, y: T2): TOut = f(x, y)
      }

    implicit val signalWithStartRight: CanConcat.Of[WorkflowMethod, SignalMethod, SignalWithStart] =
      create((_: WorkflowMethod, y: SignalMethod) => new SignalWithStart(y.signalName))

    implicit val signalWithStartLeft: CanConcat.Of[SignalMethod, WorkflowMethod, SignalWithStart] =
      create((x: SignalMethod, _: WorkflowMethod) => new SignalWithStart(x.signalName))
  }
}
