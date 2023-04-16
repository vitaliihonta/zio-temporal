package com.example.heartbeatingactivity

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

@workflowInterface
trait HeartbeatingActivityBatchWorkflow {

  /** Processes the batch of records.
    *
    * @return
    *   total number of processed records.
    */
  @workflowMethod
  def processBatch(): Int
}

/** A sample implementation of processing a batch by an activity.
  *
  * <p>An activity can run as long as needed. It reports that it is still alive through heartbeat. If the worker is
  * restarted the activity is retried after the heartbeat timeout. Temporal allows store data in heartbeat _details_.
  * These details are available to the next activity attempt. The progress of the record processing is stored in the
  * details to avoid reprocessing records from the beginning on failures.
  */
class HeartbeatingActivityBatchWorkflowImpl extends HeartbeatingActivityBatchWorkflow {

  /** Activity that is used to process batch records. The start-to-close timeout is set to a high value to support large
    * batch sizes. Heartbeat timeout is required to quickly restart the activity in case of failures. The heartbeat
    * timeout is also needed to record heartbeat details at the service.
    */
  private val recordProcessor = ZWorkflow
    .newActivityStub[RecordProcessorActivity]
    .withStartToCloseTimeout(1.hour)
    .withHeartbeatTimeout(10.seconds)
    .build

  private val logger = ZWorkflow.getLogger(getClass)

  // No special logic needed here as activity is retried automatically by the service.
  override def processBatch(): Int = {
    logger.info("Started workflow")
    val result = recordProcessor.processRecords()
    logger.info(s"Workflow result is $result")
    result
  }
}
