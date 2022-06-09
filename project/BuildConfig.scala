import sbt.Keys.scalaVersion
import sbt._

object BuildConfig extends Dependencies {

  val baseLibs = Seq(
    ScalaExt.kindProjectorCompilerPlugin,
    Temporal.self,
    Zio.self,
    Testing.scalatest,
    Distage.testKit
  ) ++ Logging.test

  val ztemporalCoreLibs = baseLibs ++ Seq(
    Scalapb.runtime,
    Utility.scalaJava8Compat,
    Utility.izumiReflect,
    Enumeratum.enumeratum % Optional
  )

  val ztemporalTestKitLibs = baseLibs ++ Seq(
    Temporal.testing,
    Jackson.scala
  )

  val testLibs = baseLibs ++ Seq(
    Distage.core
  )

  val ztemporalScalapbLibs = baseLibs ++ Seq(
    Scalapb.runtime,
    Scalapb.runtimeProtobuf,
    Utility.reflections,
    Enumeratum.enumeratum % Optional
  )

  val ztemporalDistageLibs = baseLibs ++ Seq(
    Distage.core,
    Distage.config
  )

  val examplesLibs = baseLibs ++ Seq(
    Examples.logstage,
    Examples.logstageSlf4jAdapter
  )
}

trait Dependencies {

  object version {
    val temporal   = "1.12.0"
    val zio        = "1.0.15"
    val izumi      = "1.0.8"
    val enumeratum = "1.7.0"
  }

  object org {
    val izumi    = "io.7mind.izumi"
    val beachape = "com.beachape"
    val zio      = "dev.zio"
    val temporal = "io.temporal"
  }

  object Temporal {
    val self    = org.temporal % "temporal-sdk"     % version.temporal
    val testing = org.temporal % "temporal-testing" % version.temporal
  }

  object Jackson {
    val scala = "com.fasterxml.jackson.module" %% "jackson-module-scala" % "2.13.3"
  }

  object Zio {
    val self        = org.zio %% "zio"              % version.zio
    val interopCats = org.zio %% "zio-interop-cats" % "2.5.1.1"
  }

  object Distage {
    val core    = org.izumi %% "distage-core"              % version.izumi
    val config  = org.izumi %% "distage-extension-config"  % version.izumi
    val testKit = org.izumi %% "distage-testkit-scalatest" % version.izumi % Test
  }

  object Enumeratum {
    val enumeratum = org.beachape %% "enumeratum" % version.enumeratum
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

  object ScalaExt {

    val kindProjectorCompilerPlugin = compilerPlugin(
      "org.typelevel" %% "kind-projector" % "0.13.2" cross CrossVersion.full
    )
  }

  object Logging {
    val logstage             = org.izumi %% "logstage-core"          % version.izumi
    val logstageSlf4jAdapter = org.izumi %% "logstage-adapter-slf4j" % version.izumi

    val test = List(logstage, logstageSlf4jAdapter).map(_ % Test)
  }

  object Testing {
    val scalatest = "org.scalatest" %% "scalatest" % "3.2.12" % Test
  }

  object Examples {
    val logstage             = org.izumi %% "logstage-core"          % version.izumi
    val logstageSlf4jAdapter = org.izumi %% "logstage-adapter-slf4j" % version.izumi
  }
}
