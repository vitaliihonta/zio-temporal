package com.example.error.handling

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.temporal.failure.ActivityFailure

object ArithmeticActivityImpl {
  val make: URLayer[ZActivityRunOptions[Any], ArithmeticActivity] =
    ZLayer.fromFunction(ArithmeticActivityImpl()(_: ZActivityRunOptions[Any]))
}

case class ArithmeticActivityImpl()(implicit options: ZActivityRunOptions[Any]) extends ArithmeticActivity {
  override def divide(x: Int, y: Int): Int = {
    ZActivity.run {
      for {
        _      <- ZIO.logInfo(s"divide($x, $y)")
        result <- ZIO.attempt(x / y).refineToOrDie[ArithmeticException]
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

class MathWorkflowImpl extends MathWorkflow {
  private lazy val logger = ZWorkflow.makeLogger

  private val activity: ZActivityStub.Of[ArithmeticActivity] = ZWorkflow
    .newLocalActivityStub[ArithmeticActivity](
      ZLocalActivityOptions
        .withStartToCloseTimeout(10.seconds)
        .withRetryOptions(
          ZRetryOptions.default
            .withMaximumAttempts(3)
            // Without this block, division will be retried
            .withDoNotRetry(nameOf[ArithmeticException])
        )
    )

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
      case ActivityFailure.Cause(e) =>
        logger.error(s"Caught exception inside the workflow", e)
        -1
    }
  }
}
