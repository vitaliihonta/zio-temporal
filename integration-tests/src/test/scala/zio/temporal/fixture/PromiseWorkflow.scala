package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow.ZWorkflow

@activityInterface
trait PromiseActivity {
  def foo(x: Int): Int

  def bar(x: Int): Int
}

@workflowInterface
trait PromiseWorkflow {

  @workflowMethod
  def fooBar(x: Int, y: Int): Int
}

class PromiseActivityImpl(fooFunc: Int => Int, barFunc: Int => Int) extends PromiseActivity {
  override def foo(x: Int): Int = fooFunc(x)

  override def bar(x: Int): Int = barFunc(x)
}

class PromiseWorkflowImpl extends PromiseWorkflow {

  private val activity = ZWorkflow
    .newActivityStub[PromiseActivity]
    .withStartToCloseTimeout(5.seconds)
    .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1))
    .build

  override def fooBar(x: Int, y: Int): Int = {
    val first  = ZActivityStub.executeAsync(activity.foo(x))
    val second = ZActivityStub.executeAsync(activity.bar(y))

    val result = for {
      x <- first
      y <- second
    } yield x + y

    result.run.getOrThrow
  }
}
