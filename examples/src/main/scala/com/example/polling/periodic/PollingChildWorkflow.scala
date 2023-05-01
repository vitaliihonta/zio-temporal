package com.example.polling.periodic

import zio.*
import zio.temporal.*
import zio.temporal.activity.ZActivityStub
import zio.temporal.failure.ActivityFailure
import zio.temporal.workflow.*

import scala.annotation.tailrec

@workflowInterface
trait PollingChildWorkflow {
  @workflowMethod
  def exec(pollingInterval: Duration): String
}

class PeriodicPollingChildWorkflowImpl extends PollingChildWorkflow {

  private val singleWorkflowPollAttempts = 10
  private val logger                     = ZWorkflow.makeLogger

  private val continueAsNew = ZWorkflow.newContinueAsNewStub[PollingChildWorkflow].build

  private val activities = ZWorkflow
    .newActivityStub[PollingActivities]
    .withStartToCloseTimeout(4.seconds)
    .withRetryOptions(
      ZRetryOptions.default.withMaximumAttempts(1)
    )
    .build

  override def exec(pollingInterval: Duration): String = {

    @tailrec
    def iter(i: Int): String =
      if (i >= singleWorkflowPollAttempts) {
        logger.info("Attempts exceeded, continuing as new")
        ZWorkflowContinueAsNewStub.execute(
          continueAsNew.exec(pollingInterval)
        )
        null
      } else {
        try {
          logger.info(s"Polling attempt $i")
          ZActivityStub.execute(
            activities.doPoll()
          )
        } catch {
          case e: ActivityFailure =>
            logger.warn("Attempt failed")
            ZWorkflow.sleep(pollingInterval)
            iter(i + 1)
        }
      }

    iter(0)
  }
}
