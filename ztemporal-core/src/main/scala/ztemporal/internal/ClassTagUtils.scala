package ztemporal.internal

import scala.reflect.ClassTag

private[ztemporal] object ClassTagUtils {
  def classOf[A: ClassTag]: Class[A] = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]

  def classTagOf[A: ClassTag]: ClassTag[A] = implicitly[ClassTag[A]]
}
