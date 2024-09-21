# A Working Environment for This Tutorial

The previous section mentioned the various background knowledge assumed in later sections.  This section gives specific instructions for preparing a local environment where you can experiment with the features of ZIO-Temporal for yourself as you read along.

To try the examples in this tutorial, you will need the ZIO-Temporal library as well as a running Temporal server.  There are different ways to get these, but here some suggestions.

## Install Scala, Java, and SBT

You must have Scala and SBT available, as well as the Java Development Kit.  The examples in this tutorial were tested with Scala version 3 and Java version 17.

There are multiple ways to install Scala, SBT, and Java described on the [Scala official website](https://scala-lang.org/download/).  The easiest is probably to use `Coursier`, which can install all Scala development components with a single `setup` command.  Complete installation instructions are on  [the Coursier website](https://get-coursier.io/docs/cli-installation).

## Install the Temporal Server

The Temporal server binary can be downloaded for [Linux](https://temporal.download/cli/archive/latest?platform=linux&arch=arm64) or [Windows](https://temporal.download/cli/archive/latest?platform=windows&arch=amd64), or installed on macOS using `brew install temporal`.  Detailed instructions are on [the Temporal.io website](https://learn.temporal.io/getting_started/java/dev_environment).

Once you have it installed, simply run the server with this command:

```bash
temporal server start-dev
```

It runs in the foreground (by default), so run it in its own terminal session.
Confirm it’s online by pointing your browser to [your localhost port 8233](http://localhost:8233/).

## Access the ZIO-Temporal Library

ZIO-Temporal applications depend on [the ZIO-Temporal library](https://zio.dev/ecosystem/community/zio-temporal/).  The Scala build tool [SBT](https://www.scala-sbt.org/) can manage library dependencies.  As mentioned earlier, the build file is named `build.sbt` and it goes in the root folder of the project.  The Scala source files go in the folder `src/main/scala` under the project root.

Configuration for the build goes in the `build.sbt` file, and we already showed an example of configuring the Scala version with a key-value pair:

```scala
scalaVersion := "3.5.1"
```

The `:=` operator above assigns the value on the right to the configuration setting named on the left.  Similarly, you can configure the project’s library dependencies using the `libraryDependencies` key.  The operator `+=` will add the value on the right to a collection named on the left.  To add multiple values (the library dependencies) you can use the `++=` operator applied to a collection of values on the right, in this case a [`Seq`](https://www.scala-lang.org/api/current/scala/collection/immutable/Seq$.html):

```scala
libraryDependencies ++= Seq(
  "dev.vhonta" %% "zio-temporal-core" % "0.6.1",
  "org.slf4j" % "slf4j-nop" % "2.0.16",
)
```

Placing this setting in the `build.sbt` file will add two library dependencies to the project: (1) the ZIO-Temporal development kit, and (2) a no operation [SLF4J](https://www.slf4j.org/manual.html) [Logger](https://www.slf4j.org/apidocs/org/slf4j/Logger.html) that will silence most of the logging output when running the project.  If you prefer to see the output, feel free to replace this dependency with [slf4j-simple](https://mvnrepository.com/artifact/org.slf4j/slf4j-simple), [logback-classic](https://mvnrepository.com/artifact/ch.qos.logback/logback-classic), or your favorite SLF4J logging framework provider.

With `sbt` configured to manage the library dependencies, you can compile the project with the command `sbt compile`, run it with `sbt run`, and open an interactive REPL session with `sbt console`.  The command `sbt` without arguments will open an interactive session that will recognize the `compile`, `run`, and `console` commands directly.

Now that you have your tutorial environment set up, our first topic will be how to define a Temporal Workflow.  After that we will create and run a Temporal Worker that will be listening and waiting for a command to execute the Workflow.  Then we will learn how to issue the command to Execute the Temporal Workflow programmatically, and see how to accept complex Workflow parameters in JSON format.  The final section in the tutorial will show how to include Activity Tasks in a Workflow Execution.

This is a `build.sbt` file that will work for all the examples in this tutorial:

```scala title="build.sbt"
scalaVersion := "3.5.1"

libraryDependencies ++= Seq(
  "dev.vhonta" %% "zio-temporal-core" % "0.6.1",
  "dev.zio"    %% "zio-json"          % "0.7.3",
  "org.slf4j"   % "slf4j-nop"         % "2.0.16",
)
```

Now that you have your working environment set up we can begin the main tutorial content.  In the next section we will learn how to define a Temporal Workflow.
