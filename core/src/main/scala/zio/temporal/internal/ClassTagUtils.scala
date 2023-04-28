package zio.temporal.internal

import zio.temporal.workflowMethod
import scala.reflect.ClassTag
import org.reflections.{Store, Reflections}
import org.reflections.ReflectionUtils.*
import org.reflections.scanners.Scanners
import org.reflections.util.ReflectionUtilsPredicates.withAnnotation
import org.slf4j.LoggerFactory
import java.lang.reflect.Method
import scala.jdk.CollectionConverters.*

private[zio] object ClassTagUtils {
  private val logger = LoggerFactory.getLogger(getClass)

  def classOf[A: ClassTag]: Class[A] = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]

  def classTagOf[A: ClassTag]: ClassTag[A] = implicitly[ClassTag[A]]

  final class NoWorkflowMethodException(msg: String) extends RuntimeException(msg)

  /** Type method annotations are missing during Scala 2 macro expansion in case we have only a WeakTypeTag. Therefore,
    * we must use reflection to get the workflow name
    */
  def getWorkflowType[A: ClassTag]: String = {
    val wfMethod = get[Store, Method](
      Methods
        .of(classOf[A])
        .filter(withAnnotation(Predef.classOf[workflowMethod]))
        .as(Predef.classOf[Method])
    ).asScala.headOption.getOrElse {
      throw new NoWorkflowMethodException(s"${classOf[A]} doesn't have a workflowMethod!")
    }

    val name = Option(wfMethod.getAnnotation(Predef.classOf[workflowMethod]).name())
      .filter(_.nonEmpty)
      .getOrElse(classOf[A].getSimpleName)

    logger.debug(s"Workflow interface's ${classOf[A]} workflowType is $name")
    name
  }
}
