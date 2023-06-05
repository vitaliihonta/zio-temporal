# Configuration
ZIO-Temporal integrates with the core ZIO Config. Therefore, you can configure your ZIO-Temporal easily!  
The default way that works with no extra dependencies is environment variables. Here is the list of most frequently used configuration parameters:
- `ZIO_TEMPORAL_ZWORKFLOW_SERVICE_STUBS_SERVER_URL` - Temporal server URL
- `ZIO_TEMPORAL_ZWORKER_FACTORY_MAX_WORKFLOW_THREAD_COUNT` - Maximum number of threads available for workflow execution across all workers created by the Worker Factory
- ... and more (see `ZWorkflowServiceStubsOptions` and `ZWorkerFactoryOptions`)

Note that it's possible to read from other sources. Refer to [ZIO Config docs](https://zio.dev/zio-config/) for more information

## Configuring from code
Some important configuration could be made only from code. For instance, specifying GRPC metadata (Temporal uses GRPC for server/client communication).  
Such a configuration (as well as any config that might be read by ZIO Config) can be specified from code using ZIO's aspect-based DSL (`@@` operator):  

```scala mdoc
import zio.temporal.workflow._
import io.grpc.Metadata

// Mutable Java stuff
val grpcMetadata = new Metadata()
grpcMetadata.put(Metadata.Key.of("Foo", Metadata.ASCII_STRING_MARSHALLER), "Bar")

val workflowServiceStubs = ZWorkflowServiceStubsOptions.make @@
  // "Standard" configuration
  ZWorkflowServiceStubsOptions.withServiceUrl("my-shiny-temporal-cluster:7233") @@
  // "Non-standard" configuration
  ZWorkflowServiceStubsOptions.withHeaders(grpcMetadata)
```
