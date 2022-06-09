val scala2_13 = "2.13.8"
val scala2_12 = "2.12.15"

val allScalaVersions          = List(scala2_12, scala2_13)
val documentationScalaVersion = scala2_13

scalaVersion := scala2_13

ThisBuild / organization := "com.github.vitaliihonta"
ThisBuild / version := "0.2.0"
ThisBuild / scalaVersion := scala2_13

lazy val defaultSettings = Seq(
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

lazy val root = project
  .in(file("."))
  .settings(defaultSettings)
  .settings(
    name := "ztemporal-root",
    publishArtifact := false
  )
  .aggregate(
    examples
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

lazy val docs = projectMatrix
  .in(file("doc-template"))
  .settings(defaultSettings)
  .settings(
    name := "doc",
    publishArtifact := false,
    mdocIn := file("doc-template/src/main/mdoc"),
    mdocOut := file("docs"),
    moduleName := "ztemporal-doc",
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
  .settings(defaultSettings)
  .settings(crossCompileSettings)
  .settings(
    libraryDependencies += BuildConfig.ScalaReflect.macros.value
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `ztemporal-core` = projectMatrix
  .in(file("ztemporal-core"))
  .dependsOn(`ztemporal-macro-utils`)
  .settings(defaultSettings)
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
  .settings(defaultSettings)
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
  .settings(defaultSettings)
  .settings(crossCompileSettings)
  .settings(
    name := "tests",
    publishArtifact := false,
    libraryDependencies ++= BuildConfig.testLibs
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val `ztemporal-scalapb` = projectMatrix
  .in(file("ztemporal-scalapb"))
  .settings(defaultSettings)
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
  .settings(defaultSettings)
  .settings(crossCompileSettings)
  .settings(
    libraryDependencies ++= BuildConfig.ztemporalDistageLibs ++ Seq(
      BuildConfig.ScalaReflect.macros.value
    )
  )
  .jvmPlatform(scalaVersions = allScalaVersions)

lazy val examples = project
  .in(file("examples"))
  .settings(defaultSettings)
  .settings(
    name := "examples",
    scalaVersion := scala2_13,
    Compile / PB.targets := Seq(
      scalapb.gen(
        flatPackage = true,
        grpc = false
      ) -> (Compile / sourceManaged).value / "scalapb"
    ),
    libraryDependencies ++= BuildConfig.examplesLibs
  )
  .dependsOn(
    `ztemporal-core`.jvm(scala2_13),
    `ztemporal-testkit`.jvm(scala2_13),
    `ztemporal-scalapb`.jvm(scala2_13),
    `ztemporal-distage`.jvm(scala2_13)
  )
