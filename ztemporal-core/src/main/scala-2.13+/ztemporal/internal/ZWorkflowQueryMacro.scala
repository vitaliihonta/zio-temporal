package ztemporal.internal

import io.temporal.workflow.QueryMethod
import ztemporal.func._
import ztemporal.utils.macros.MacroUtils
import ztemporal.workflow.ZWorkflowStub

import scala.reflect.macros.blackbox

class ZWorkflowQueryMacro(override val c: blackbox.Context) extends MacroUtils(c) {
  import c.universe._

  private val QueryMethod = typeOf[QueryMethod].dealias

  def queryImpl0[Q: WeakTypeTag, R: WeakTypeTag](f: Expr[Q => R]): Tree = {
    val Q = weakTypeOf[Q].dealias
    val R = weakTypeOf[R].dealias

    val methodName      = extractMethodSelector0(f.tree).getOrElse(error(s"$f is not method selector"))
    val queryMethodName = getQueryType(Q, methodName)

    val ZWorkflowQuery0 = weakTypeOf[ZWorkflowQuery0[R]]

    q"""
       new $ZWorkflowQuery0($currentStub, classOf[$R], $queryMethodName)
       """ debugged s"query0[$Q, $R]"
  }

  def queryImpl1[Q: WeakTypeTag, A: WeakTypeTag, R: WeakTypeTag](f: Expr[Q => A => R]): Tree = {
    val Q = weakTypeOf[Q].dealias
    val A = weakTypeOf[A].dealias
    val R = weakTypeOf[R].dealias

    val methodName      = extractMethodSelector1(f.tree).getOrElse(error(s"$f is not method selector"))
    val queryMethodName = getQueryType(Q, methodName)

    val ZWorkflowQuery1 = weakTypeOf[ZWorkflowQuery1[A, R]]

    q"""
       new $ZWorkflowQuery1($currentStub, classOf[$R], $queryMethodName)
       """ debugged s"query[$Q, $A, $R]"
  }

  def queryImpl2[Q: WeakTypeTag, A: WeakTypeTag, B: WeakTypeTag, R: WeakTypeTag](f: Expr[Q => (A, B) => R]): Tree = {
    val Q = weakTypeOf[Q].dealias
    val A = weakTypeOf[A].dealias
    val B = weakTypeOf[B].dealias
    val R = weakTypeOf[R].dealias

    val methodName      = extractMethodSelector2(f.tree).getOrElse(error(s"$f is not method selector"))
    val queryMethodName = getQueryType(Q, methodName)

    val ZWorkflowQuery2 = weakTypeOf[ZWorkflowQuery2[A, B, R]]

    q"""
       new $ZWorkflowQuery2($currentStub, classOf[$R], $queryMethodName)
       """ debugged s"query[$Q, $A, $B, $R]"
  }

  private def getQueryType(workflow: Type, methodName: TermName): String =
    getMethodAnnotation(workflow, methodName, QueryMethod).children.tail
      .collectFirst { case NamedArg(_, Literal(Constant(queryName: String))) =>
        queryName
      }
      .getOrElse(methodName.toString)

  private def currentStub: Tree =
    if (!(c.prefix.tree.tpe <:< weakTypeOf[ZWorkflowStub]))
      error("query should be called on ZWorkflowStub")
    else
      c.prefix.tree
}
