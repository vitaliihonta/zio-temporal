package com.example.error.handling

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.temporal.failure._

object SafeMath {
  case class MathError(error: String)
  def divide(x: Int, y: Int): IO[MathError, Int] =
    if (y == 0) ZIO.fail(MathError("Zero division"))
    else ZIO.succeed(x / y)
}

object TypedArithmeticActivityImpl {
  val make: URLayer[ZActivityRunOptions[Any], ArithmeticActivity] =
    ZLayer.fromFunction(TypedArithmeticActivityImpl()(_: ZActivityRunOptions[Any]))
}

case class TypedArithmeticActivityImpl()(implicit options: ZActivityRunOptions[Any]) extends ArithmeticActivity {
  override def divide(x: Int, y: Int): Int = {
    ZActivity.run {
      for {
        _      <- ZIO.logInfo(s"divide($x, $y)")
        result <- SafeMath.divide(x, y)
      } yield result
    }
  }
  override def multiply(x: Int, y: Int): Int = {
    ZActivity.run {
      for {
        _ <- ZIO.logInfo(s"divide($x, $y)")
      } yield x * y
    }
  }
}

class TypedMathWorkflowImpl extends MathWorkflow {
  private lazy val logger = ZWorkflow.makeLogger

  private val activity: ZActivityStub.Of[ArithmeticActivity] = ZWorkflow
    .newLocalActivityStub[ArithmeticActivity]
    .withStartToCloseTimeout(10.seconds)
    .withRetryOptions(
      ZRetryOptions.default
        .withMaximumAttempts(3)
        // Without this block, division will be retried
        .withDoNotRetry(nameOf[SafeMath.MathError])
    )
    .build

  override def formula(a: Int): Int = {
    val twice = ZActivityStub.execute(
      activity.multiply(a, a)
    )
    try {
      ZActivityStub.execute(
        activity.divide(twice, 0)
      )
    } catch {
      // Example of handling errors in the workflow
      case ActivityFailure.Cause(error: ApplicationFailure) =>
        // By default, typed error value is propagated as ApplicationFailure details
        val details = error.getDetailsAs[SafeMath.MathError]
        logger.error(s"Caught exception inside the workflow error=$details")
        -1
    }
  }
}
