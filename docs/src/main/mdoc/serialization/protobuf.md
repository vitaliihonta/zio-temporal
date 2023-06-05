# Protobuf

ZIO-Temporal provides with the `zio-temporal-protobuf` module allowing to use _Protobuf_ transport.

## Defining protobuf files

When using Protobuf, it's first required to write protobuf message definition:

```protobuf
syntax = "proto2";

package com.example.workflow;

message WorkflowParams {
  required string message = 1;
  // Unix time
  required int64 processingTime = 2;
}

// ZIO Temporal ships some non-standard data types, such as UUID
import "zio-temporal.proto";

message WorkflowResult {
  required zio.temporal.protobuf.UUID processId = 1;
}
```

Scalapb compiles such a message into a case class similar to this one:

```scala mdoc
case class WorkflowParams(message: String, processingTime: Long)

case class WorkflowResult(processId: zio.temporal.protobuf.UUID)
```

It can be then used in workflow interface definitions:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.workflow._

@workflowInterface
trait SampleWorkflow {
  @workflowMethod
  def process(params: WorkflowParams): WorkflowResult
}
```

When implementing the workflow, you might need to convert from protobuf representation to your own.  
For instance, it's much easier operating with `java.time` types rather than Unix time represented as `Long`.  
Here `zio.temporal.protobuf.syntax` comes to the rescue!

```scala mdoc:silent
import zio.temporal.protobuf.syntax._
import java.time.LocalDateTime

class SampleWorkflowImpl extends SampleWorkflow {
  private val logger = ZWorkflow.makeLogger
  
  override def process(params: WorkflowParams): WorkflowResult = {
    // Converts from Protobuf representation
    val processingTime = params.processingTime.fromProto[LocalDateTime]
    // Always use this method to get the current time
    // Otherwise, the workflow won't be deterministic
    val now = ZWorkflow.currentTimeMillis.toLocalDateTime()
    
    val lag = java.time.Duration.between(processingTime, now)
    logger.info(s"The lag is $lag")
    // Do stuff
    // ...
    val processId = ZWorkflow.randomUUID
    // Converts into Protobuf representation
    WorkflowResult(processId.toProto)
  }
}
```

**Notes**
- `import zio.temporal.protobuf.syntax._` is required
- `fromProto` extension method converts the data type **from** the Protobuf representation
- `toProto` extension method converts the data type **into** the Protobuf representation