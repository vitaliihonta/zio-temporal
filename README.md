[![zio-temporal-core Scala version support](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core/latest-by-scala-version.svg?platform=jvm&dummy=true)](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core)
![Build status](https://github.com/vitaliihonta/zio-temporal/actions/workflows/publish.yaml/badge.svg)

# ZIO Temporal

This is an integration with [Temporal workflow](https://temporal.io) based on Java SDK and ZIO.  
It allows you to define and use workflows in a Scala way!

The documentation and examples could be found on project's web site.  
https://zio-temporal.vhonta.dev

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
