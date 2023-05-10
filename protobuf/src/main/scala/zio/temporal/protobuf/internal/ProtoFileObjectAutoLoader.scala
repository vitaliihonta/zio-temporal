package zio.temporal.protobuf.internal

import org.reflections.Reflections
import org.reflections.util.ClasspathHelper
import org.reflections.util.ConfigurationBuilder
import org.reflections.vfs.Vfs
import org.slf4j.LoggerFactory
import scalapb.GeneratedFileObject

import java.net.URL
import scala.jdk.CollectionConverters.*

object ProtoFileObjectAutoLoader {
  private val logger           = LoggerFactory.getLogger(getClass)
  private val scalaModuleField = "MODULE$"

  def loadFromClassPath(
    classLoader:     ClassLoader,
    excludePrefixes: Set[String] = standardExcludePrefixes
  ): List[GeneratedFileObject] = {
    logger.trace(s"Provided exclude prefixes: ${excludePrefixes.mkString("[", ",", "]")}")
    val reflections = new Reflections(
      new ConfigurationBuilder()
        .filterInputsBy((s: String) => excludeRule(excludePrefixes)(s))
        .setUrls(buildScanPath(classLoader))
    )

    val loadedSubTypes = reflections.getSubTypesOf(classOf[GeneratedFileObject]).asScala.toList
    logger.trace(s"Found subtypes of GeneratedFileObject: ${loadedSubTypes.mkString("[", ",", "]")}")
    val results = loadedSubTypes.map(getGeneratedObjectInstance)
    logger.info(
      s"Loaded ${results.size} GeneratedFileObject(s): ${results.map(showGeneratedFileObject).mkString("[", ",", "]")}"
    )
    results
  }

  private def buildScanPath(classLoader: ClassLoader): java.util.Collection[URL] = {
    val base = ClasspathHelper.forClassLoader(classLoader).asScala.toList ++
      ClasspathHelper.forJavaClassPath().asScala

    val defaultSupportedURLTypes = Vfs.getDefaultUrlTypes.asScala.toList

    // Filter out unsupported ClassPath URLs in order to avoid noisy Reflections logs
    base.filter(url => defaultSupportedURLTypes.exists(_.matches(url))).asJavaCollection
  }

  def standardExcludePackages: Set[String] =
    Set(
      "META-INF",
      "akka",
      "android",
      "cats",
      "com.cronutils",
      "com.fasterxml",
      "com.google.api",
      "com.google.cloud",
      "com.google.common",
      "com.google.errorprone",
      "com.google.geo",
      "com.google.gson",
      "com.google.logging",
      "com.google.longrunning",
      "com.google.protobuf",
      "com.google.rpc",
      "com.google.thirdparty",
      "com.google.type",
      "com.sun",
      "com.sun",
      "com.thoughtworks",
      "com.typesafe.config",
      "com.uber.m3",
      "distage",
      "gogoproto",
      "io.grpc",
      "io.micrometer",
      "io.perfmark",
      "io.temporal",
      "io.temporal",
      "izumi",
      "java",
      "javax",
      "jdk",
      "logstage",
      "magnolia",
      "mercator",
      "net.sf.cglib",
      "org.HdrHistogram",
      "org.LatencyUtils",
      "org.apache.ivy",
      "org.checkerframework",
      "org.codehaus.mojo",
      "org.reflections",
      "org.scalatools",
      "org.slf4j",
      "pureconfig",
      "sbt",
      "scala",
      "sun",
      "xsbt",
      "xsbti",
      "zio",
      "zio.temporal"
    )

  def standardExcludePrefixes: Set[String] = {
    val excludePackages = standardExcludePackages
    val filesInPackages = excludePackages.map(packagePrefixToDirPrefix)
    Set("module-info.class") ++ excludePackages ++ filesInPackages
  }

  def excludeRule(excludes: Set[String])(s: String): Boolean =
    s.endsWith(".class") && !excludes.exists(s.startsWith)

  def packagePrefixToDirPrefix(pkg: String): String =
    pkg.replace(".", "/") + "/"

  private def getGeneratedObjectInstance(cls: Class[_ <: GeneratedFileObject]): GeneratedFileObject =
    cls
      .getDeclaredField(scalaModuleField)
      .get(null)
      .asInstanceOf[GeneratedFileObject]

  private def showGeneratedFileObject(f: GeneratedFileObject): String =
    s"GeneratedFileObject(class=${f.getClass.getName}, file=${f.scalaDescriptor.fullName})"
}
