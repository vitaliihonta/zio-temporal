val scala213 = "2.13.8"
val scala212 = "2.12.15"

val allScalaVersions          = List(scala212, scala213)
val documentationScalaVersion = scala213

ThisBuild / scalaVersion  := scala213
ThisBuild / organization  := "com.github.vitaliihonta"
ThisBuild / version       := "0.1.0-RC1"
ThisBuild / versionScheme := Some("early-semver")

val publishSettings = Seq(
  publishTo            := sonatypePublishToBundle.value,
  publishMavenStyle    := true,
  sonatypeProfileName  := "com.github.vitaliihonta",
  organizationHomepage := Some(url("https://github.com/vitaliihonta")),
  homepage             := Some(url("https://github.com/vitaliihonta")),
  licenses := Seq(
    "Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
  ),
  scmInfo := Some(
    ScmInfo(
      url(s"https://github.com/vitaliihonta/ztemporal"),
      s"scm:git:https://github.com/vitaliihonta/ztemporal.git",
      Some(s"scm:git:git@github.com:vitaliihonta/ztemporal.git")
    )
  ),
  developers := List(
    Developer(
      id = "vitaliihonta",
      name = "Vitalii Honta",
      email = "vitalii.honta@gmail.com",
      url = new URL("https://github.com/vitaliihonta")
    )
  ),
  sonatypeCredentialHost := "oss.sonatype.org"
)

val coverageSettings = Seq(
  //  Keys.fork in org.jacoco.core.
  jacocoAggregateReportSettings := JacocoReportSettings(
    title = "ZTemporal Coverage Report",
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
    (config / unmanagedSourceDirectories) += {
      val sourceDir = (config / sourceDirectory).value
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((2, n)) if n >= 13 => sourceDir / "scala-2.13+"
        case _                       => sourceDir / "scala-2.13-"
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
    name := "ztemporal-root"
  )
  .aggregate(
    `ztemporal-macro-utils`.projectRefs ++
      `ztemporal-core`.projectRefs ++
      `ztemporal-testkit`.projectRefs ++
      `ztemporal-scalapb`.projectRefs ++
      `ztemporal-distage`.projectRefs ++
      tests.projectRefs ++
      docs.projectRefs: _*
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
    `ztemporal-core`.jvm(scala213),
    `ztemporal-testkit`.jvm(scala213),
    `ztemporal-scalapb`.jvm(scala213),
    `ztemporal-distage`.jvm(scala213),
    tests.jvm(scala213)
  )

lazy val docs = projectMatrix
  .in(file("doc-template"))
  .settings(baseSettings, noPublishSettings)
  .settings(
    name            := "doc",
    publishArtifact := false,
    mdocIn          := file("doc-template/src/main/mdoc"),
    mdocOut         := file("docs"),
    moduleName      := "ztemporal-doc",
    mdocVariables := Map(
      "VERSION" -> version.value
    )
  )
  .enablePlugins(MdocPlugin)
  .jvmPlatform(scalaVersions = allScalaVersions)
  .dependsOn(
    `ztemporal-core`,
    `ztemporal-testkit`,
    `ztemporal-scalapb`,
    `ztemporal-distage`
  )

lazy val `ztemporal-macro-utils` = projectMatrix
  .in(file("ztemporal-macro-utils"))
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    libraryDependencies += BuildConfig.ScalaReflect.macros.value
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `ztemporal-core` = projectMatrix
  .in(file("ztemporal-core"))
  .dependsOn(`ztemporal-macro-utils`)
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    libraryDependencies ++= BuildConfig.ztemporalCoreLibs ++ Seq(
      BuildConfig.ScalaReflect.macros.value
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `ztemporal-testkit` = projectMatrix
  .in(file("ztemporal-testkit"))
  .dependsOn(`ztemporal-core`)
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    libraryDependencies ++= BuildConfig.ztemporalTestKitLibs ++ Seq(
      BuildConfig.ScalaReflect.macros.value
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val tests = projectMatrix
  .in(file("tests"))
  .dependsOn(
    `ztemporal-core`,
    `ztemporal-testkit` % "compile->compile;test->test",
    `ztemporal-scalapb`,
    `ztemporal-distage`
  )
  .settings(baseSettings, coverageSettings, noPublishSettings, crossCompileSettings)
  .settings(
    name := "tests",
    libraryDependencies ++= BuildConfig.testLibs
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `ztemporal-scalapb` = projectMatrix
  .in(file("ztemporal-scalapb"))
  .settings(baseLibSettings)
  .dependsOn(`ztemporal-core`)
  .settings(crossCompileSettings)
  .settings(
    libraryDependencies ++= BuildConfig.ztemporalScalapbLibs,
    libraryDependencies += BuildConfig.ScalaReflect.runtime.value,
    Compile / PB.targets := Seq(
      scalapb.gen(
        flatPackage = true,
        grpc = false
      ) -> (Compile / sourceManaged).value / "scalapb"
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `ztemporal-distage` = projectMatrix
  .in(file("ztemporal-distage"))
  .dependsOn(`ztemporal-core`, `ztemporal-macro-utils`)
  .settings(baseLibSettings)
  .settings(crossCompileSettings)
  .settings(
    libraryDependencies ++= BuildConfig.ztemporalDistageLibs ++ Seq(
      BuildConfig.ScalaReflect.macros.value
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val examples = project
  .in(file("examples"))
  .settings(baseSettings, noPublishSettings)
  .settings(
    name         := "examples",
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
    `ztemporal-core`.jvm(scala213),
    `ztemporal-testkit`.jvm(scala213),
    `ztemporal-scalapb`.jvm(scala213),
    `ztemporal-distage`.jvm(scala213)
  )
