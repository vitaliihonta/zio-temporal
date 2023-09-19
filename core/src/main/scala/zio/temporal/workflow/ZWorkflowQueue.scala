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

  def offer(e: E): Boolean

  def put(e: E): Unit

  def cancellablePut(e: E): Unit

  def offer(e: E, timeout: Duration): Boolean

  def cancellableOffer(e: E, timeout: Duration): Boolean

}

class ZQueueConsumer[E] private[zio] (toJava: QueueConsumer[E]) {

  def take(): E =
    toJava.take()

  def cancellableTake(): E =
    toJava.cancellableTake()

  def poll(): Option[E] =
    Option(toJava.poll())

  def peek(): Option[E] =
    Option(toJava.peek())

  def poll(timeout: zio.Duration): Option[E] =
    Option(toJava.poll(timeout))

  def cancellablePoll(timeout: zio.Duration): Option[E] =
    Option(toJava.cancellablePoll(timeout))

  def map[R](f: E => R): ZQueueConsumer[R] =
    new ZQueueConsumer(toJava.map(e => f(e)))

}
