package zio.temporal.internal

import zio.*
import zio.temporal.extras.ZLayerAspect
import zio.temporal.internalApi

@internalApi
abstract class ConfigurationCompanion[Configurable: Tag] {
  final type Configure = ZLayerAspect[Nothing, Any, Nothing, Any, Nothing, Configurable]

  private[zio] def configure(f: Configurable => Configurable): Configure =
    new ZLayerAspect[Nothing, Any, Nothing, Any, Nothing, Configurable] {
      override def apply[R >: Nothing <: Any, E >: Nothing <: Any, A >: Nothing <: Configurable](
        layer:          ZLayer[R, E, A]
      )(implicit trace: Trace
      ): ZLayer[R, E, A] = {
        layer.map(_.update[Configurable](f))
      }
    }
}
