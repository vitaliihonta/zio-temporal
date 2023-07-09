# Jackson

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal jackson" />
  <meta name="keywords" content="ZIO Temporal jackson, Scala Temporal jackson" />
</head>

Jackson is the default serialization format. No extra configuration is usually required for workflow/activity parameters to be serialized.  
ZIO-Temporal goes with:
- Jackson-Scala module
- Java 8 data time module

**(1)** If custom serialization logic is required, refer to Jackson documentation.  

**(2)** If providing a custom `ObjectMapper` is required (like this):
```scala mdoc
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.json.JsonMapper
// Scala module
import com.fasterxml.jackson.module.scala.DefaultScalaModule

val CustomMapper: ObjectMapper = JsonMapper
  .builder()
  .addModule(DefaultScalaModule)
  // Do whatever you need
  .build()
```

Override the default `ZWorkflowClientOptions` by providing a new `DataConverter`:

```scala mdoc
import zio._
import zio.temporal.json._
import zio.temporal.workflow.ZWorkflowClientOptions
import io.temporal.common.converter.DataConverter

// Create a data converter
val dataConverter: DataConverter = JacksonDataConverter.make(CustomMapper)

// Create a new ZWorkflowClientOptions
val optionsLayer = 
  ZWorkflowClientOptions.make @@ 
    // override any configuration if needed
    ZWorkflowClientOptions.withDataConverter(dataConverter)
```
