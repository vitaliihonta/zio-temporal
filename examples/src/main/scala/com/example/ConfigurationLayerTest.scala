package com.example

import zio._

// Something configurable, but not a config (e.g. SSLContext or whaterver)
trait SomeConfig
// Alternative implementations
object SomeConfig {
  val default: ULayer[SomeConfig] = ZLayer.succeed(DefaultSomeConfig)
}

// The default one, provided by framework

case object DefaultSomeConfig extends SomeConfig
// A custom one
case object CustomSomeConfig extends SomeConfig

// A holder for lots of options, which is updatable
case class ConfigurationHolder private (someConfig: SomeConfig) {
  // Specify different options
  def withSomeConfig(other: SomeConfig): ConfigurationHolder =
    new ConfigurationHolder(other)

  // NOTE: needs something like 'build' that will be used by a framework
}
object ConfigurationHolder {
  val default: ULayer[ConfigurationHolder] =
    ZLayer.make[ConfigurationHolder](
      ZLayer.fromFunction(ConfigurationHolder(_)),
      // Default configs
      SomeConfig.default
    )

  // Aspects allowing to modify the Holder
  def withSomeConfig(
    config: SomeConfig
  ): ZIOAspect[Nothing, ConfigurationHolder, Nothing, Any, Nothing, Any] =
    new ZIOAspect[Nothing, ConfigurationHolder, Nothing, Any, Nothing, Any] {
      override def apply[R >: Nothing <: ConfigurationHolder, E >: Nothing <: Any, A >: Nothing <: Any](
        zio:            ZIO[R, E, A]
      )(implicit trace: Trace
      ): ZIO[R, E, A] = {
        ZIO.environmentWithZIO[R] { env =>
          val newEnv = env.update[ConfigurationHolder](_.withSomeConfig(config))
          zio.provideEnvironment(newEnv)
        }
      }
    }
}

object MainTest extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    // Program which uses ConfigurationHolder
    val program: ZIO[ConfigurationHolder, Any, Unit] = {
      val theRun = for {
        holder <- ZIO.service[ConfigurationHolder]
        _      <- ZIO.logInfo(s"Service is ${holder.someConfig}")
      } yield ()

      // Program may 'override' configuration if needed
      theRun @@ ConfigurationHolder.withSomeConfig(CustomSomeConfig)
    }

    // ConfigurationHolder is provided by framework
    program.provide(
      ConfigurationHolder.default
    )
  }
}
