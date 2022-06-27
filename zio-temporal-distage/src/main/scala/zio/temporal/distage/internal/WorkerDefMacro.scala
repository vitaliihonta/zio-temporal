package zio.temporal.distage.internal

import izumi.distage.constructors.macros.AnyConstructorMacro
import izumi.distage.model.definition.dsl.ModuleDefDSL.MakeDSL
import zio.temporal._
import zio.temporal.distage.RegisteredActivity
import zio.temporal.distage.RegisteredWorkflow
import zio.temporal.distage.ZWorkerDef
import zio.temporal.utils.macros.MacroUtils

import scala.reflect.macros.blackbox

class WorkerDefMacro(override val c: blackbox.Context) extends MacroUtils(c) {
  import c.universe._

  private val WorkflowInterface = typeOf[workflow]
  private val ActivityInterface = typeOf[activity]

  private val anyClass    = typeOf[Class[AnyRef]].dealias
  private val anyWorkflow = typeOf[RegisteredWorkflow[AnyRef]].dealias
  private val anyActivity = typeOf[RegisteredActivity[AnyRef]].dealias

  def registerWorkflowImplementationImpl[A: WeakTypeTag]: Tree = {
    val A = weakTypeOf[A].dealias

    if (!extendsWorkflow(A))
      error(s"$A is not a workflow: it should extend a trait with @$WorkflowInterface annotation")

    if (!isConcrete(A))
      error(s"workflow $A is abstract, should be a concrete class or object")

    q"""
       $currentWorkerDef.many[$currentWorkerDef.Mine[${anyWorkflow.typeConstructor}]]
         .add(
           $currentWorkerDef.Mine(new $anyWorkflow(classOf[$A].asInstanceOf[$anyClass], None))
         )
     """ debugged s"Added workflow implementation $A"
  }

  def registerWorkflowFactoryImpl[A: WeakTypeTag]: Tree = {
    val A = weakTypeOf[A].dealias

    if (!extendsWorkflow(A))
      error(s"$A is not a workflow: it should extend a trait with @$WorkflowInterface annotation")

    if (isConcrete(A))
      error(s"workflow $A is concrete, should be a trait or abstract class")

    q"""
       new $currentWorkerDef.MakeWorkflowImplementationDsl[$A](classOf[$A])
     """ debugged s"Adding workflow $A"
  }

  def registerActivityImpl[A: WeakTypeTag]: Tree = {
    val A = weakTypeOf[A].dealias

    if (!isActivity(A))
      error(s"$A is not an activity: missing @$ActivityInterface annotation")

    val activity = freshTermName("activity")

    val makeImpl = AnyConstructorMacro.make[MakeDSL, A](c)

    q"""
       {
         $currentWorkerDef.many[$currentWorkerDef.Mine[${anyActivity.typeConstructor}]]
           .add(
             ($activity: $A) => $currentWorkerDef.Mine(new $anyActivity($activity))
           );
         $makeImpl
       }
     """ debugged s"Added activity $A"
  }

  private def currentWorkerDef: Tree =
    if (!(c.prefix.tree.tpe <:< weakTypeOf[ZWorkerDef]))
      error("makeWorkflow should be used only inside WorkerDef")
    else
      c.prefix.tree

  private def isConcrete(tpe: Type): Boolean =
    !tpe.typeSymbol.isAbstract

  private def extendsWorkflow(tpe: Type): Boolean =
    tpe.baseClasses.exists(
      _.annotations.exists(_.tree.tpe <:< WorkflowInterface)
    )

  private def isActivity(tpe: Type): Boolean =
    tpe.typeSymbol.annotations.exists(_.tree.tpe <:< ActivityInterface)
}
