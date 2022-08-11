# Overview

`zio-temporal` is an integration with [Temporal workflow](https://temporal.io) based on Java SDK and ZIO.  
It allows you to run workflows in the Scala way!

[TL;DR intro about Temporal workflow](https://youtu.be/2HjnQlnA5eY)

<head>
  <meta charset="UTF-8" />
  <meta name="author" content="Vitalii Honta" />
  <meta name="description" content="Build invincible apps with ZIO and Temporal" />
  <meta name="keywords" content="scala, zio, temporal, zio-temporal, workflow management" />
</head>

## Installation

```sbt
// Core
libraryDependencies += "@ORGANIZATION@" %% "zio-temporal-core" % "@VERSION@"

// Protobuf transport
libraryDependencies += "@ORGANIZATION@" %% "zio-temporal-protobuf" % "@VERSION@"

// Testkit
libraryDependencies += "@ORGANIZATION@" %% "zio-temporal-testkit" % "@VERSION@"
```

## Use cases
TBD

## Modules

1. **zio-temporal-core** - ZIO integration and basic wrappers that bring type safety to your workflows.  
   Allows you to use arbitrary ZIO code in your activities
2. **zio-temporal-protobuf** - integration with [ScalaPB](https://scalapb.github.io/) which allows you to
   use [Protobuf](https://developers.google.com/protocol-buffers)  
   as a transport layer protocol for communication with Temporal cluster
3. **zio-temporal-testkit** - wrappers for `temporal-testkit` module which allows to test your workflows locally in unit tests. 
