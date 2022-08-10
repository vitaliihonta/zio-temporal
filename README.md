[![zio-temporal-core Scala version support](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core)
![Build status](https://github.com/vitaliihonta/zio-temporal/actions/workflows/publish.yaml/badge.svg)
[![codecov](https://codecov.io/gh/vitaliihonta/zio-temporal/branch/main/graph/badge.svg?token=T8NBC4R360)](https://codecov.io/gh/vitaliihonta/zio-temporal)

# ZIO Temporal

This is an integration with [Temporal workflow](https://temporal.io) based on Java SDK and ZIO.  
It allows you to define and use workflows in a Scala way!

[TL;DR intro about Temporal workflow](https://youtu.be/2HjnQlnA5eY)

## Modules

1. **zio-temporal-core** - ZIO integration and basic wrappers that bring type safety to your workflows.  
   Allows you to use arbitrary ZIO code in your activities
2. **zio-temporal-protobuf** - integration with [ScalaPB](https://scalapb.github.io/) which allows you to
   use [Protobuf](https://developers.google.com/protocol-buffers)  
   as a transport layer protocol for communication with Temporal cluster
3. **zio-temporal-testkit** - wrappers for `temporal-testkit` module which allows to test your workflows locally in unit tests. 

## Installation

```sbt
// Core
libraryDependencies += "dev.vhonta" %% "zio-temporal-core" % "<VERSION>"

// Protobuf transport
libraryDependencies += "dev.vhonta" %% "zio-temporal-protobuf" % "<VERSION>"

// Testkit
libraryDependencies += "dev.vhonta" %% "zio-temporal-testkit" % "<VERSION>"
```

## Examples

You can find the source code of example workflow in [examples directory](./examples)

### How to run examples

1. Start temporal server locally with predefined [docker-compose file](./examples/docker-compose.yaml):

```shell
docker-compose -f examples/docker-compose.yaml up -d
```

2. Run example from sbt:

```shell
sbt examples/run
```
