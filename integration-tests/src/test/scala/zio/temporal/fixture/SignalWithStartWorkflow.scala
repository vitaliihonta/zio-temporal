package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow.ZWorkflow
import scala.annotation.tailrec

@workflowInterface
trait SignalWithStartWorkflow {
  @workflowMethod
  def echoServer(): Int

  @queryMethod
  def messages: List[String]

  @signalMethod
  def echo(message: String): Unit

  @signalMethod
  def stop(): Unit
}

class SignalWithStartWorkflowImpl extends SignalWithStartWorkflow {
  private case class State(messages: List[String], isStopped: Boolean)

  private val state = ZWorkflowState.make(
    State(Nil, isStopped = false)
  )

  override def echoServer(): Int = {
    @tailrec
    def go(processed: Int): Unit = {
      val currentState = state.snapshot
      if (!currentState.isStopped) {
        if (currentState.messages.size > processed) {
          val newMessages = currentState.messages.take(currentState.messages.size - processed)
          newMessages.foreach(println)
          ZWorkflow.sleep(100.millis)
          go(processed + newMessages.size)
        } else {
          ZWorkflow.sleep(100.millis)
        }
      }
    }

    go(processed = 0)
    state.snapshotOf(_.messages.size)
  }

  override def messages: List[String] = state.snapshotOf(_.messages).reverse

  override def echo(message: String): Unit =
    state.update(s => s.copy(messages = message :: s.messages))

  override def stop(): Unit =
    state.update(_.copy(isStopped = true))
}
