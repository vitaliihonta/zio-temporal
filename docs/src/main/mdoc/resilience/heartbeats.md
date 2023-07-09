# Activity Heartbeats

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal heartbeats" />
  <meta name="keywords" content="ZIO Temporal heartbeats, Scala Temporal heartbeats" />
</head>

An [Activity Heartbeat](https://docs.temporal.io/activities#activity-heartbeat) is a ping from the Worker Process that is executing the Activity to the Temporal Cluster.  
Each Heartbeat informs the Temporal Cluster that the Activity Execution is making progress and the Worker has not crashed. If the Cluster does not receive a Heartbeat within a _Heartbeat Timeout_ time period,  
the Activity will be considered failed and another Activity Task Execution may be scheduled according to the _Retry Policy_.

Heartbeats may not always be sent to the Cluster — they may be throttled by the Worker.

Activity Cancellations are delivered to Activities from the Cluster when they Heartbeat. Activities that don't Heartbeat can't receive a Cancellation. Heartbeat throttling may lead to Cancellation getting delivered later than expected.

Heartbeats can contain a _details_ field describing the Activity's current progress. If an Activity gets retried, the Activity can access the _details_ from the last Heartbeat that was sent to the Cluster.

## Example

In this example, we'll implement a record processor that periodically reads records from the storage & commits offsets of processed records. The processor will be able to recover to the latest processed offset in case of a failure. You can fine the full example [here](https://github.com/vitaliihonta/zio-temporal/tree/main/examples/src/main/scala/com/example/heartbeatingactivity)

Let's start with some basic definitions that will be required for the whole demonstration:
```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.workflow._

/** Record to process. A real application would add a use case specific data. */
case class SingleRecord(id: Int)

/** Helper class that is used to iterate over a list of records.
  */
trait RecordLoader {

  /** Returns the next record.
    */
  def getRecord(offset: Int): UIO[Option[SingleRecord]]
}

class RecordLoaderImpl extends RecordLoader {
  private val RecordCount = 1000

  override def getRecord(offset: Int): UIO[Option[SingleRecord]] = {
    if (offset >= RecordCount) ZIO.none
    else ZIO.some(SingleRecord(offset))
  }
}

/** A helper class that implements record processing. */
trait RecordProcessor {

  /** Processes a single record.
   */
  def processRecord(record: SingleRecord): Task[Unit]
}

class RecordProcessorImpl extends RecordProcessor {
  override def processRecord(record: SingleRecord): Task[Unit] = {
    for {
      randomDelay <- ZIO.randomWith(_.nextIntBetween(100, 500))
      _           <- ZIO.sleep(randomDelay.millis)
      _           <- ZIO.logInfo(s"Processed record=$record")
    } yield ()
  }
}
```

## Sending & reading heartbeats
First, define an activity interface. 
```scala mdoc:silent
@activityInterface
trait RecordProcessorActivity {

  /** Processes all records in the dataset */
  def processRecords(): Int
}
```

To Heartbeat an Activity Execution, use the `ZActivityExecutionContext`:

```scala mdoc:silent
class RecordProcessorActivityImpl(
  recordLoader:     RecordLoader,
  recordProcessor:  RecordProcessor
)(implicit options: ZActivityOptions[Any])
    extends RecordProcessorActivity {

  override def processRecords(): Int = {
    val context: ZActivityExecutionContext = 
      ZActivity.executionContext

    def processLoop(offset: Int): Task[Int] = {
      // Poll records
      recordLoader.getRecord(offset).flatMap {
        case None => ZIO.succeed(offset)
        case Some(record) =>
          recordProcessor.processRecord(record) *>
            // Send a heartbeat
            context.heartbeat(offset) *>
            processLoop(offset + 1)
      }
    }

    ZActivity.run {
      for {
        // Get latest heartbeat
        heartbeatDetails <- context.getHeartbeatDetails[Int]
        // If missing, start from 0
        initialOffset = heartbeatDetails.getOrElse(0)
        _           <- ZIO.logInfo(s"Processing records since offset=$initialOffset")
        finalOffset <- processLoop(initialOffset)
      } yield finalOffset
    }
  }
}
```

**Notes**:  

**(1)** To heartbeat an Activity Execution, use `ZActivityExecutionContext.heartbeat` method
- The method takes a `details` argument that represents latest progress of the Activity Execution. 
- This method can take a variety of types such as an exception object, custom object, or string.

**(2)** If the Activity Execution times out, the last Heartbeat details are included in the thrown `TimeoutFailure`, which can be caught by the calling Workflow.  
The Workflow can then use the details information to pass to the next Activity invocation if needed.

**(3)** In the case of Activity retries, the last Heartbeat's details are available and can be extracted from the last failed attempt by using `ZActivityExecutionContext.getHeartbeatDetails[<DetailsType>]`

## Heartbeat timeout
A [Heartbeat Timeout](https://docs.temporal.io/activities#heartbeat-timeout) works in conjunction with Activity Heartbeats.

To set a Heartbeat Timeout, use `withHeartbeatTimeout` when creating the Activity stub:

```scala mdoc:silent
@workflowInterface
trait HeartbeatingActivityBatchWorkflow {

  /** Processes the batch of records.
    */
  @workflowMethod
  def processBatch(): Int
}

class HeartbeatingActivityBatchWorkflowImpl extends HeartbeatingActivityBatchWorkflow {
  
  private val recordProcessor: ZActivityStub.Of[RecordProcessorActivity] = ZWorkflow
    .newActivityStub[RecordProcessorActivity]
    .withStartToCloseTimeout(1.hour)
    // Heartbeat timeout
    .withHeartbeatTimeout(10.seconds)
    .build

  private val logger = ZWorkflow.makeLogger

  // No special logic needed here as activity is retried automatically by the service.
  override def processBatch(): Int = {
    logger.info("Started workflow")
    val result = ZActivityStub.execute(
      recordProcessor.processRecords()
    )
    logger.info(s"Workflow result is $result")
    result
  }  
}
```
