package zio.temporal.internal

import zio.temporal.{activityMethod, workflowMethod}

import scala.reflect.ClassTag
import org.slf4j.LoggerFactory

private[zio] object ClassTagUtils {
  private val logger = LoggerFactory.getLogger(getClass)

  def classOf[A: ClassTag]: Class[A] = implicitly[ClassTag[A]].runtimeClass.asInstanceOf[Class[A]]

  def classTagOf[A: ClassTag]: ClassTag[A] = implicitly[ClassTag[A]]

  final class NoWorkflowMethodException(msg: String) extends RuntimeException(msg)

  final class NoActivityMethodException(msg: String) extends RuntimeException(msg)

  /** Type method annotations are missing during Scala 2 macro expansion in case we have only a WeakTypeTag. Therefore,
    * we must use reflection to get the workflow name
    */
  def getWorkflowType[A: ClassTag]: String = {
    val wfMethod = classOf[A].getMethods
      .filterNot(_.getAnnotation(classOf[workflowMethod]) == null)
      .headOption
      .getOrElse {
        throw new NoWorkflowMethodException(s"${classOf[A]} doesn't have a workflowMethod!")
      }

    val name = Option(wfMethod.getAnnotation(Predef.classOf[workflowMethod]).name())
      .filter(_.nonEmpty)
      .getOrElse(classOf[A].getSimpleName)

    logger.debug(s"Workflow interface's ${classOf[A]} workflowType is $name")
    name
  }

  def getActivityType(cls: Class[_], methodName: String): String = {
    val actMethods = cls.getMethods.filter(_.getName == methodName).toList

    if (actMethods.isEmpty) {
      throw new NoActivityMethodException(s"$cls doesn't have an activity method '$methodName'!")
    }

    logger.trace(s"Found activity methods with name=$methodName: $actMethods")

    val name = actMethods
      // It may have overrides
      .flatMap(actMethod => Option(actMethod.getAnnotation(Predef.classOf[activityMethod])))
      .headOption
      .flatMap(ann => Option(ann.name()))
      .filter(_.nonEmpty)
      .getOrElse(methodName.capitalize)

    logger.trace(s"Activity interface's $cls method=$methodName has activity name $name")
    name
  }
}
