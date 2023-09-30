package zio.temporal.workflow

import io.temporal.workflow.{QueueConsumer, WorkflowQueue}
import zio.Duration

final class ZWorkflowQueue[E] private[zio] (val toJava: WorkflowQueue[E])
    extends ZQueueConsumer[E](toJava)
    with ZQueueProducer[E] {

  override def offer(e: E): Boolean =
    toJava.offer(e)

  override def put(e: E): Unit =
    toJava.put(e)

  override def cancellablePut(e: E): Unit =
    toJava.cancellablePut(e)

  override def offer(e: E, timeout: zio.Duration): Boolean =
    toJava.offer(e, timeout)

  override def cancellableOffer(e: E, timeout: zio.Duration): Boolean =
    toJava.cancellableOffer(e, timeout)
}

trait ZQueueProducer[E] {

  /** Inserts the specified element into this queue if it is possible to do so immediately without violating capacity
    * restrictions, returning true upon success and false if no space is currently available.
    *
    * @param e
    *   the element to add
    * @return
    *   true if the element was added to this queue, else false
    * @throws ClassCastException
    *   if the class of the specified element prevents it from being added to this queue
    * @throws NullPointerException
    *   if the specified element is null
    * @throws IllegalArgumentException
    *   if some property of the specified element prevents it from being added to this queue
    */
  def offer(e: E): Boolean

  /** Inserts the specified element into this queue, waiting if necessary for space to become available. It is not
    * unblocked in case of the enclosing CancellationScope cancellation. Use [[cancellablePut]] instead.
    *
    * @param e
    *   the element to add
    * @throws ClassCastException
    *   if the class of the specified element prevents it from being added to this queue
    * @throws NullPointerException
    *   if the specified element is null
    * @throws IllegalArgumentException
    *   if some property of the specified element prevents it from being added to this queue
    */
  def put(e: E): Unit

  /** Inserts the specified element into this queue, waiting if necessary for space to become available.
    *
    * @param e
    *   the element to add
    * @throws io.temporal.failure.CanceledFailure
    *   if surrounding [[io.temporal.workflow.CancellationScope]] is canceled while waiting
    * @throws ClassCastException
    *   if the class of the specified element prevents it from being added to this queue
    * @throws NullPointerException
    *   if the specified element is null
    * @throws IllegalArgumentException
    *   if some property of the specified element prevents it from being added to this queue
    */
  def cancellablePut(e: E): Unit

  /** Inserts the specified element into this queue, waiting up to the specified wait time if necessary for space to
    * become available. It is not unblocked in case of the enclosing CancellationScope cancellation. Use
    * [[cancellableOffer]] instead.
    *
    * @param e
    *   the element to add
    * @param timeout
    *   how long to wait before giving up
    * @return
    *   true if successful, or false if the specified waiting time elapses before space is available
    * @throws ClassCastException
    *   if the class of the specified element prevents it from being added to this queue
    * @throws NullPointerException
    *   if the specified element is null
    * @throws IllegalArgumentException
    *   if some property of the specified element prevents it from being added to this queue
    */
  def offer(e: E, timeout: Duration): Boolean

  /** Inserts the specified element into this queue, waiting up to the specified wait time if necessary for space to
    * become available.
    *
    * @param e
    *   the element to add
    * @param timeout
    *   how long to wait before giving up
    * @return
    *   true if successful, or false if the specified waiting time elapses before space is available
    * @throws io.temporal.failure.CanceledFailure
    *   if surrounding [[io.temporal.workflow.CancellationScope]] is canceled while waiting
    * @throws ClassCastException
    *   if the class of the specified element prevents it from being added to this queue
    * @throws NullPointerException
    *   if the specified element is null
    * @throws IllegalArgumentException
    *   if some property of the specified element prevents it from being added to this queue
    */
  def cancellableOffer(e: E, timeout: Duration): Boolean

}

class ZQueueConsumer[E] private[zio] (toJava: QueueConsumer[E]) {

  /** Retrieves and removes the head of this queue, waiting if necessary until an element becomes available. It is not
    * unblocked in case of the enclosing * CancellationScope cancellation. Use [[cancellableTake]] instead.
    *
    * @return
    *   the head of this queue
    */
  def take(): E =
    toJava.take()

  /** Retrieves and removes the head of this queue, waiting if necessary until an element becomes available.
    *
    * @return
    *   the head of this queue
    * @throws io.temporal.failure.CanceledFailure
    *   if surrounding [[io.temporal.workflow.CancellationScope]] is canceled while waiting
    */
  def cancellableTake(): E =
    toJava.cancellableTake()

  /** Retrieves and removes the head of this queue if it is not empty without blocking.
    *
    * @return
    *   the head of this queue wrapped in Some, or None if the queue is empty
    */
  def poll(): Option[E] =
    Option(toJava.poll())

  /** Retrieves the head of this queue keeping it in the queue if it is not empty without blocking.
    *
    * @return
    *   the head of this queue wrapped in Some, or None if the queue is empty
    */
  def peek(): Option[E] =
    Option(toJava.peek())

  /** Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary for an element to
    * become available. It is not unblocked in case of the enclosing CancellationScope cancellation. Use
    * [[cancellablePoll]] instead.
    *
    * @param timeout
    *   how long to wait before giving up.
    * @return
    *   the head of this queue wrapped in Some, or None if the specified waiting time elapses before an element is
    *   available
    */
  def poll(timeout: zio.Duration): Option[E] =
    Option(toJava.poll(timeout))

  /** Retrieves and removes the head of this queue, waiting up to the specified wait time if necessary for an element to
    * become available.
    *
    * @param timeout
    *   how long to wait before giving up
    * @return
    *   the head of this queue wrapped in Some, or None if the specified waiting time elapses before an element is
    *   available
    * @throws io.temporal.failure.CanceledFailure
    *   if surrounding [[io.temporal.workflow.CancellationScope]] is canceled while waiting
    */
  def cancellablePoll(timeout: zio.Duration): Option[E] =
    Option(toJava.cancellablePoll(timeout))

  /** Returns a queue consisting of the results of applying the given function to the elements of this queue.
    *
    * @param f
    *   a non-interfering, stateless function to apply to each element
    * @return
    *   the new queue backed by this one.
    */
  def map[R](f: E => R): ZQueueConsumer[R] =
    new ZQueueConsumer(toJava.map(e => f(e)))

}
