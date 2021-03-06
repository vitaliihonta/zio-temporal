val scala213 = "2.13.8"
val scala212 = "2.12.15"

val allScalaVersions          = List(scala212, scala213)
val documentationScalaVersion = scala213

ThisBuild / scalaVersion           := scala213
ThisBuild / organization           := "dev.vhonta"
ThisBuild / version                := "0.1.0-RC3"
ThisBuild / versionScheme          := Some("early-semver")
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository     := "https://s01.oss.sonatype.org/service/local"

val publishSettings = Seq(
  publishTo            := sonatypePublishToBundle.value,
  publishMavenStyle    := true,
  organizationHomepage := Some(url("https://vhonta.dev")),
  homepage             := Some(url("https://vhonta.dev")),
  licenses := Seq(
    "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
  ),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/vitaliihonta/zio-temporal"),
      s"scm:git:https://github.com/vitaliihonta/zio-temporal.git",
      Some(s"scm:git:git@github.com:vitaliihonta/zio-temporal.git")
    )
  ),
  developers := List(
    Developer(
      id = "vitaliihonta",
      name = "Vitalii Honta",
      email = "vitalii.honta@gmail.com",
      url = new URL("https://github.com/vitaliihonta")
    )
  )
)
val coverageSettings = Seq(
  //  Keys.fork in org.jacoco.core.
  jacocoAggregateReportSettings := JacocoReportSettings(
    title = "ZIO Temporal Coverage Report",
    subDirectory = None,
    thresholds = JacocoThresholds(),
    formats = Seq(JacocoReportFormats.ScalaHTML, JacocoReportFormats.XML), // note XML formatter
    fileEncoding = "utf-8"
  )
)

lazy val baseProjectSettings = Seq(
  scalacOptions ++= {
    val baseOptions = Seq(
      "-language:implicitConversions",
      "-language:higherKinds"
    )
    val crossVersionOptions = CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, y)) if y < 13 => Seq("-Ypartial-unification")
      case _                      => Seq.empty[String]
    }
    baseOptions ++ crossVersionOptions
  }
)

val crossCompileSettings: Seq[Def.Setting[_]] = {
  def crossVersionSetting(config: Configuration) =
    (config / unmanagedSourceDirectories) ++= {
      val sourceDir = (config / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, _))            => List(sourceDir / "scala-3")
        case Some((2, n)) if n >= 13 => List(sourceDir / "scala-2", sourceDir / "scala-2.13+")
        case _                       => List(sourceDir / "scala-2", sourceDir / "scala-2.13-")
      }
    }

  Seq(
    crossVersionSetting(Compile),
    crossVersionSetting(Test)
  )
}

val noPublishSettings = Seq(
  publish / skip := true,
  publish        := {}
)

val baseSettings    = baseProjectSettings
val baseLibSettings = baseSettings ++ publishSettings ++ coverageSettings

lazy val root = project
  .in(file("."))
  .settings(baseSettings, noPublishSettings)
  .settings(
    name := "zio-temporal-root"
  )
  .aggregate(
    core.projectRefs ++
      testkit.projectRefs ++
      protobuf.projectRefs ++
      `integration-tests`.projectRefs: _*
  )
  .aggregate(
    examples,
    coverage
  )

lazy val coverage = project
  .in(file("./.coverage"))
  .settings(baseSettings, coverageSettings)
  .settings(
    publish / skip := true,
    publish        := {}
  )
  .aggregate(
    core.jvm(scala213),
    testkit.jvm(scala213),
    protobuf.jvm(scala213),
    `integration-tests`.jvm(scala213)
  )

lazy val core = projectMatrix
  .in(file("core"))
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    name := "zio-temporal-core",
    libraryDependencies ++= BuildConfig.coreLibs ++ Seq(
      BuildConfig.ScalaReflect.macros.value
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val testkit = projectMatrix
  .in(file("testkit"))
  .dependsOn(core)
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    name := "zio-temporal-testkit",
    libraryDependencies ++= BuildConfig.testkitLibs ++ Seq(
      BuildConfig.ScalaReflect.macros.value
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `integration-tests` = projectMatrix
  .in(file("integration-tests"))
  .dependsOn(
    core,
    testkit % "compile->compile;test->test",
    protobuf
  )
  .settings(baseSettings, coverageSettings, noPublishSettings, crossCompileSettings)
  .settings(
    libraryDependencies ++= BuildConfig.testLibs,
    testFrameworks := BuildConfig.Zio.testFrameworks
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val protobuf = projectMatrix
  .in(file("protobuf"))
  .settings(baseLibSettings)
  .dependsOn(core)
  .settings(crossCompileSettings)
  .settings(
    name := "zio-temporal-protobuf",
    libraryDependencies ++= BuildConfig.protobufLibs,
    libraryDependencies += BuildConfig.ScalaReflect.runtime.value,
    Compile / PB.targets := Seq(
      scalapb.gen(
        flatPackage = true,
        grpc = false
      ) -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val examples = project
  .in(file("examples"))
  .settings(baseSettings, noPublishSettings)
  .settings(
    scalaVersion := scala213,
    Compile / PB.targets := Seq(
      scalapb.gen(
        flatPackage = true,
        grpc = false
      ) -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= BuildConfig.examplesLibs
  )
  .dependsOn(
    core.jvm(scala213),
    testkit.jvm(scala213),
    protobuf.jvm(scala213)
  )
