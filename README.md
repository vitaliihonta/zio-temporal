[![Support ukraine](https://img.shields.io/static/v1?label=United24&message=Support%20Ukraine&color=lightgrey&link=https%3A%2F%2Fu24.gov.ua&logo=data%3Aimage%2Fpng%3Bbase64%2CiVBORw0KGgoAAAANSUhEUgAAASwAAADICAYAAABS39xVAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAJcEhZcwAADsMAAA7DAcdvqGQAAANKSURBVHhe7dZBThRhFEbRnx1IgvtFiIoxbgemOHLAhAoJ1QyaBahroKxqE%2BMS6iZncPKSbwE3b4yr6W58en4Z148zwC5tjbqabrdgvZ59PS5nn2eAfVobtbbquAXrcBquJ4B9Wht1%2BrQEC9g9wQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8gQLCBDsIAMwQIyBAvIECwgQ7CADMECMgQLyBAsIEOwgAzBAjIEC8j4F6zL%2BTA%2BHpfxYR0A9mhr1OXzPC5u7g%2Fvv%2F1YLr58B9ilU6Nu7ufx6%2BH88Hs6X9YLsEtbo34%2BvJvH29M4LC9jWZ4Admpt1NqqNVjTGqz5bFkmgJ1aG%2FX2KFhAgWABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVkCBaQIVhAhmABGYIFZAgWkCFYQIZgARmCBWQIFpAhWECGYAEZggVk%2FBes1%2BX4dwDYpbVRa6uOW7Du3p7Hy1YvgF3aGjWN2z9qCgwkg1n6XwAAAABJRU5ErkJggg%3D%3D)](https://u24.gov.ua)
[![zio-temporal-core Scala version support](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core/latest-by-scala-version.svg?platform=jvm&dummy=true)](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core)
[![zio-temporal-core Latest snapshot](https://img.shields.io/nexus/s/https/s01.oss.sonatype.org/dev.vhonta/zio-temporal-core_3.svg "Sonatype Snapshots")](https://s01.oss.sonatype.org/content/repositories/snapshots/dev/vhonta/zio-temporal-core_3/ "Sonatype Snapshots")
![Build status](https://github.com/vitaliihonta/zio-temporal/actions/workflows/ci.yaml/badge.svg)
[![codecov](https://codecov.io/gh/vitaliihonta/zio-temporal/branch/main/graph/badge.svg?token=0BF6UA46PQ)](https://codecov.io/gh/vitaliihonta/zio-temporal)

# ZIO Temporal

This is an integration with [Temporal workflow](https://temporal.io) based on Java SDK and ZIO.  
It allows you to define and use workflows in a Scala way!

The documentation and examples can be found on the project's website.  
https://zio-temporal.vhonta.dev

## Examples

You can find the source code of the example workflow in [examples directory](./examples).
To create & start a Temporal cluster, it is required to install [Temporal CLI](https://docs.temporal.io/cli).  

### How to run examples

1. Start the Temporal cluster

```shell
make start-temporal
```

2. Run example from sbt:

```shell
sbt examples/run
```
