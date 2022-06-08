package ztemporal.fixture

import io.temporal.activity.ActivityInterface
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import ztemporal.ZRetryOptions
import ztemporal.workflow._
import ztemporal.promise._

import scala.concurrent.duration._

@ActivityInterface
trait PromiseActivity {
  def foo(x: Int): Int

  def bar(x: Int): Int
}

@WorkflowInterface
trait PromiseWorkflow {

  @WorkflowMethod
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
    val first  = ZPromise.fromEither(Right(activity.foo(x)))
    val second = ZPromise.fromEither(Right(activity.bar(y)))

    val result = for {
      x <- first
      y <- second
    } yield x + y

    result.run.value
  }
}
