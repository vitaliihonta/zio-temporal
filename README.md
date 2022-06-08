# ZTemporal

ZTemporal is a "tiny" wrapper around Java SDK for [temporal workflow](https://temporal.io).  
It contains the following modules:

```scala
// CORE
"<org>" %% "ztemporal-core" % "@VERSION@"

// ScalaPB integration
"<org>" %% "ztemporal-scalapb" % "@VERSION@"

// Distage integration**** integration
"<org>" %% "ztemporal-distage" % "@VERSION@"
```

## How to run examples

1. Start temporal server locally with predefined [docker-compose file](./examples/docker-compose.yaml):

```shell
docker-compose -f examples/docker-compose.yaml up -d
```

2. Run example from sbt:

```shell
sbt examples/run
```