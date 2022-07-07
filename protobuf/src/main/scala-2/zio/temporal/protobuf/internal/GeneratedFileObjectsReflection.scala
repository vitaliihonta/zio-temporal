package zio.temporal.protobuf.internal

import scala.reflect.runtime.universe.*
import scalapb.GeneratedFileObject
import zio.temporal.experimentalApi
import scala.util.Try

@experimentalApi
private[zio] object GeneratedFileObjectsReflection {
  def reflect(
    loadedSubTypes: List[Class[_ <: GeneratedFileObject]],
    classLoader:    ClassLoader
  ): List[GeneratedFileObject] = {
    val mirror = runtimeMirror(classLoader)
    loadedSubTypes.flatMap { cls =>
      val clsSymbol = mirror.classSymbol(cls)
      if (!clsSymbol.isModuleClass) None
      else {
        val moduleSymbol = clsSymbol.owner.info.decl(clsSymbol.name.toTermName).asModule
        Try {
          mirror
            .reflectModule(moduleSymbol)
            .instance
            .asInstanceOf[GeneratedFileObject]
        }.toOption
      }
    }
  }
}
