package com.example.error.handling

import io.temporal.failure.{ActivityFailure, ApplicationFailure}
import org.slf4j.LoggerFactory
import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*

object ErrorTypes {
  val ZeroDivision = "ZeroDivision"
}

case class ErrorData(divisor: Int)

@activityInterface
trait ArithmeticActivity {
  def divide(x:   Int, y: Int): Int
  def multiply(x: Int, y: Int): Int
}

class ArithmeticActivityImpl extends ArithmeticActivity {
  private lazy val logger = LoggerFactory.getLogger(getClass)
  override def divide(x: Int, y: Int): Int = {
    logger.info(s"divide($x, $y)")
    try {
      x / y
    } catch {
      case _: ArithmeticException =>
        throw ApplicationFailure.newFailure(
          "Zero division",
          ErrorTypes.ZeroDivision,
          ErrorData(divisor = x)
        )
    }
  }
  override def multiply(x: Int, y: Int): Int = {
    logger.info(s"multiply($x, $y)")
    x * y
  }
}

@workflowInterface
trait MathWorkflow {
  @workflowMethod
  def formula(a: Int): Int
}

class MathWorkflowImpl extends MathWorkflow {
  private lazy val logger = LoggerFactory.getLogger(getClass)

  private val activity = ZWorkflow
    .newActivityStub[ArithmeticActivity]
    .withStartToCloseTimeout(10.seconds)
    .withRetryOptions(
      ZRetryOptions.default
        .withMaximumAttempts(3)
        // Without this block, division will be retried
        .withDoNotRetry(ErrorTypes.ZeroDivision)
    )
    .build

  override def formula(a: Int): Int = {
    val twice = activity.multiply(a, a)
    try {
      activity.divide(twice, 0)
    } catch {
      // Example of handling errors in the workflow
      case e: ActivityFailure =>
        val errorDetails = e.getCause match {
          case af: ApplicationFailure =>
            // NOTE: jackson-module-scala or protobuf integration is required to serialize/deserialize it here
            Some(af.getDetails.get(classOf[ErrorData]))
          case _ => None
        }
        logger.error(s"Caught exception inside the workflow details=$errorDetails", e)
        -1
    }
  }
}
