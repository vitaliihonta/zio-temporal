package zio.temporal.workflow

import io.temporal.workflow.CancellationScope

/** Handle to a cancellation scope created through [[ZWorkflow.newCancellationScope]] or
  * [[ZWorkflow.newDetachedCancellationScope]]. Supports explicit cancelling of the code a cancellation scope wraps. The
  * code in the CancellationScope has to be executed using [[run]] method.
  */
final class ZCancellationScope private[zio] (val toJava: CancellationScope) {

  /** Cancels the scope as well as all its children */
  def cancel(): Unit = toJava.cancel()

  /** Cancels the scope as well as all its children.
    *
    * @param reason
    *   human readable reason for the cancellation. Becomes message of the CanceledException thrown.
    */
  def cancel(reason: String): Unit = toJava.cancel(reason)

  /** Returns cancellation reason if was specified
    *
    * @return
    *   optional cancellation reason
    */
  def cancellationReason: Option[String] = Option(toJava.getCancellationReason)

  /** Is scope was asked to cancel through [[cancel]] or by a parent scope.
    *
    * @return
    *   whether request is canceled or not.
    */
  def isCancelRequested: Boolean = toJava.isCancelRequested

  /** When set to false parent thread cancellation causes this one to get canceled automatically. When set to true only
    * call to [[cancel()]] leads to this scope cancellation.
    */
  def isDetached: Boolean = toJava.isDetached

  /** Use this promise to perform cancellation of async operations.
    *
    * @return
    *   promise that becomes ready when scope is canceled. It contains reason value or null if none was provided.
    */
  def cancellationRequest: ZAsync[String] =
    new ZAsync.Impl[String](toJava.getCancellationRequest)

  /** Executes the code specified in this cancellation scope */
  def run(): Unit = toJava.run()
}

object ZCancellationScope {

  /** @return current cancellation scope */
  def current: ZCancellationScope = new ZCancellationScope(CancellationScope.current())
}
