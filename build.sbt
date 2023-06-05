import BuildConfig._

val scala212 = "2.12.17"
val scala213 = "2.13.10"
val scala3   = "3.3.0"

val allScalaVersions          = List(scala212, scala213, scala3)
val documentationScalaVersion = scala213

ThisBuild / scalaVersion           := scala213
ThisBuild / organization           := "dev.vhonta"
ThisBuild / versionScheme          := Some("early-semver")
ThisBuild / sonatypeCredentialHost := "s01.oss.sonatype.org"
ThisBuild / sonatypeRepository     := "https://s01.oss.sonatype.org/service/local"

val publishSettings = Seq(
  publishTo            := sonatypePublishToBundle.value,
  publishMavenStyle    := true,
  organizationHomepage := Some(url("https://vhonta.dev")),
  homepage             := Some(url("https://zio-temporal.vhonta.dev")),
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

lazy val coverageSettings = Seq(
  coverageExcludedPackages := "com\\.example\\..*;.*\\.JavaTypeTag;.*\\.JavaTypeTag\\..*;.*\\.TypeIsSpecified;" +
    "zio\\.temporal\\.internal\\.*Macro*;"
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

val baseSettings    = baseProjectSettings ++ coverageSettings
val baseLibSettings = baseSettings ++ publishSettings

lazy val root = project
  .in(file("."))
  .settings(baseSettings, noPublishSettings, unidocSettings)
  .settings(
    name := "zio-temporal-root"
  )
  .aggregate(
    core.projectRefs ++
      testkit.projectRefs ++
      protobuf.projectRefs ++
      `integration-tests`.projectRefs ++
      examples.projectRefs: _*
  )
  .aggregate(
    docs
  )
  .enablePlugins(ScalaUnidocPlugin)

lazy val docs = project
  .in(file("docs"))
  .settings(
    name := "zio-temporal-docs",
    baseSettings,
    mdocSettings,
    noPublishSettings,
    scalaVersion := scala213,
    libraryDependencies ++= docsLibs
  )
  .dependsOn(
    core.jvm(scala213),
    protobuf.jvm(scala213),
    testkit.jvm(scala213)
  )
  .enablePlugins(MdocPlugin, DocusaurusPlugin)

lazy val core = projectMatrix
  .in(file("core"))
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    name := "zio-temporal-core",
    libraryDependencies ++= coreLibs ++ {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) =>
          Seq(
            ScalaReflect.macros.value
          ) ++ coreLibsScala2
        case _ => Nil
      }
    }
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val testkit = projectMatrix
  .in(file("testkit"))
  .dependsOn(core)
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    name := "zio-temporal-testkit",
    libraryDependencies ++= testkitLibs
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `integration-tests` = projectMatrix
  .in(file("integration-tests"))
  .dependsOn(
    core,
    testkit % "compile->compile;test->test",
    protobuf
  )
  .settings(baseSettings, noPublishSettings, crossCompileSettings)
  .settings(
    libraryDependencies ++= testLibs ++ {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) => testLibsScala2
        case _            => Nil
      }
    },
    testFrameworks ++= Zio.testFrameworks,
    (Test / parallelExecution) := false,
    Compile / PB.targets := Seq(
      scalapb.gen(
        flatPackage = true,
        grpc = false
      ) -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val protobuf = projectMatrix
  .in(file("protobuf"))
  .settings(baseLibSettings)
  .dependsOn(core)
  .settings(crossCompileSettings)
  .settings(
    name := "zio-temporal-protobuf",
    libraryDependencies ++= protobufLibs ++ {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, _)) => protobufScala2Libs
        case _            => Nil
      }
    },
    Compile / PB.targets := Seq(
      scalapb.gen(
        flatPackage = true,
        grpc = false
      ) -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val examples = projectMatrix
  .in(file("examples"))
  .settings(baseSettings, noPublishSettings)
  .settings(
    coverageEnabled := false,
    Compile / PB.targets := Seq(
      scalapb.gen(
        flatPackage = true,
        grpc = false
      ) -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= examplesLibs
  )
  .dependsOn(
    core,
    testkit,
    protobuf
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

// MDOC
lazy val mdocSettings = Seq(
  mdocIn := file("docs/src/main/mdoc"),
  mdocVariables := Map(
    "VERSION"      -> version.value,
    "ORGANIZATION" -> organization.value,
    "EMAIL"        -> developers.value.head.email
  ),
  crossScalaVersions := Seq(scalaVersion.value)
)

lazy val unidocSettings = Seq(
  cleanFiles += (ScalaUnidoc / unidoc / target).value,
  ScalaUnidoc / unidoc / unidocProjectFilter := inProjects(
    core.jvm(scala213),
    protobuf.jvm(scala213),
    testkit.jvm(scala213)
  ),
  ScalaUnidoc / unidoc / target := (LocalRootProject / baseDirectory).value / "website" / "static" / "api",
  ScalaUnidoc / unidoc / scalacOptions ++= Seq(
    "-sourcepath",
    (LocalRootProject / baseDirectory).value.getAbsolutePath,
    "-doc-title",
    "ZIO Temporal",
    "-doc-version",
    s"v${(ThisBuild / version).value}"
  )
)

val updateSiteVariables = taskKey[Unit]("Update site variables")
ThisBuild / updateSiteVariables := {
  val file =
    (ThisBuild / baseDirectory).value / "website" / "variables.js"

  val variables =
    Map[String, String](
      "organization"           -> (ThisBuild / organization).value,
      "latestVersion"          -> (ThisBuild / version).value,
      "downloadReportsBaseUrl" -> "https://zio-temporal.vhonta.dev/assets"
    )

  val fileHeader =
    "// Generated by sbt. Do not edit directly."

  val fileContents =
    variables.toList
      .sortBy { case (key, _) => key }
      .map { case (key, value) => s"  $key: '$value'" }
      .mkString(s"$fileHeader\nmodule.exports = {\n", ",\n", "\n};\n")

  IO.write(file, fileContents)
}

// MISC
lazy val noPublishSettings =
  publishSettings ++ Seq(
    publish / skip  := true,
    publishArtifact := false
  )
