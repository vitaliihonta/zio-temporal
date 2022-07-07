import sbt.Keys.scalaVersion

import sbt._

object BuildConfig extends Dependencies {

  val baseLibs = Seq(
    Temporal.self,
    Zio.self
  )

  val coreLibs = baseLibs ++ Seq(
    Scalapb.runtime,
    Utility.scalaJava8Compat,
    Utility.izumiReflect
  )

  val coreLibsScala2 = Seq(
    Enumeratum.enumeratum % Optional
  )

  val testkitLibs = baseLibs ++ Seq(
    Temporal.testing,
    Jackson.scala
  )

  val testLibs = baseLibs ++ Seq(
    Zio.test,
    Zio.testSbt,
    Logging.zio % Test
  )

  val protobufLibs = baseLibs ++ Seq(
    Scalapb.runtime,
    Scalapb.runtimeProtobuf,
    Utility.reflections
  )

  val protobufScala2Libs = Seq(
    Enumeratum.enumeratum % Optional
  )

  val examplesLibs = baseLibs ++ Seq(
    Logging.zio,
    Logging.zioSlf4j,
    Logging.logback
  )
}

trait Dependencies {

  private object versions {
    val temporal   = "1.12.0"
    val zio        = "2.0.0"
    val zioLogging = "2.0.0"
    val enumeratum = "1.7.0"
  }

  object org {
    val beachape = "com.beachape"
    val zio      = "dev.zio"
    val temporal = "io.temporal"
  }

  object Temporal {
    val self    = org.temporal % "temporal-sdk"     % versions.temporal
    val testing = org.temporal % "temporal-testing" % versions.temporal
  }

  object Jackson {
    val scala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3"
  }

  object Zio {
    val self           = org.zio %% "zio"          % versions.zio
    val test           = org.zio %% "zio-test"     % versions.zio % Test
    val testSbt        = org.zio %% "zio-test-sbt" % versions.zio % Test
    val testFrameworks = Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  }

  object Enumeratum {
    val enumeratum = org.beachape %% "enumeratum" % versions.enumeratum
  }

  object Scalapb {
    val runtime         = "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion
    val runtimeProtobuf = runtime                 % "protobuf"
  }

  object Utility {
    val scalaJava8Compat = "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2"
    val izumiReflect     = org.zio                  %% "izumi-reflect"      % "2.1.0"
    val reflections      = "org.reflections"         % "reflections"        % "0.10.2"
  }

  object ScalaReflect {

    val macros = Def.setting {
      "org.scala-lang" % "scala-reflect" % scalaVersion.value % Provided
    }

    val runtime = Def.setting {
      "org.scala-lang" % "scala-reflect" % scalaVersion.value
    }
  }

  object Typelevel {
    val kindProjector = compilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
    )
  }

  object Logging {
    val zio      = org.zio         %% "zio-logging"       % versions.zioLogging
    val zioSlf4j = org.zio         %% "zio-logging-slf4j" % versions.zioLogging
    val logback  = "ch.qos.logback" % "logback-classic"   % "1.2.11"
  }
}
