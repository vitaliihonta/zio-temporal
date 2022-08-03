package zio.temporal.workflow

import zio.*
import io.temporal.client.WorkflowClient
import scala.compat.java8.OptionConverters._
import scala.quoted.*

trait ZWorkflowStubProxySyntax extends Any {
  def toJava: WorkflowClient

  /** Creates workflow untyped client stub for a known execution. Use it to send signals or queries to a running
    * workflow. Do not call methods annotated with @WorkflowMethod.
    *
    * @see
    *   [[ZWorkflowStub]]
    */
  inline def newWorkflowStubProxy[A: IsConcreteType](
    workflowId: String,
    runId:      Option[String] = None
  ): UIO[ZWorkflowStub.Proxy[A]] =
    ${ ZWorkflowStubProxySyntax.newWorkflowStubProxyImpl[A]('toJava, 'workflowId, 'runId) }
}

object ZWorkflowStubProxySyntax {
  trait Test
  class Foo
  // TODO: implement without cast?
  def newWorkflowStubProxyImpl[A: Type](
    toJava:     Expr[WorkflowClient],
    workflowId: Expr[String],
    runId:      Expr[Option[String]]
  )(using q:    Quotes
  ): Expr[UIO[ZWorkflowStub.Proxy[A]]] = {
    import q.reflect.*
    val aSymbol = TypeRepr.of[A].typeSymbol
    println(aSymbol.declaredMethods)
    val A = Type.of[A]
    println('{ new Foo with Test }.asTerm)

    val t = AndType(
      TypeRepr.of[ZWorkflowStub],
      TypeRepr.of[A]
    )

    // TODO: work around this
    val tree = Apply(
      Select(
        New(TypeTree.of(using t.asType)),
        TypeRepr.of[ZWorkflowStub].typeSymbol.primaryConstructor
      ),
      Nil
    ).asExprOf[ZWorkflowStub & A]

    println(tree.show)

//    val mixinTree = Inlined(
//      Ident(ZWorkflowStubProxySyntax$),
//      List(),
//      Block(
//        List(
//          TypeDef(
//            $anon,
//            Template(
//              DefDef(
//                initt,
//                List(List()),
//                TypeTree(TypeRef(TermRef(ThisType(TypeRef(NoPrefix, moduleclassroot)), objectscala), Unit)),
//                EmptyTree
//              ),
//              List(Apply(Select(New(Ident(Foo)), initt), List()), Ident(Test)),
//              ValDef(_, EmptyTree, EmptyTree),
//              List()
//            )
//          )
//        ),
//        Typed(
//          Apply(Select(New(Ident($anon)), initt), List()),
//          TypeTree(
//            AndType(
//              TypeRef(
//                ThisType(
//                  TypeRef(
//                    TermRef(ThisType(TypeRef(NoPrefix, moduleclasstemporal)), objectworkflow),
//                    ZWorkflowStubProxySyntax$
//                  )
//                ),
//                Foo
//              ),
//              TypeRef(
//                ThisType(
//                  TypeRef(
//                    TermRef(ThisType(TypeRef(NoPrefix, moduleclasstemporal)), objectworkflow),
//                    ZWorkflowStubProxySyntax$
//                  )
//                ),
//                Test
//              )
//            )
//          )
//        )
//      )
//    )

    val result = '{
//      new $A {}
      ZIO.succeed {
        ZWorkflowStub.Proxy[A](
          new ZWorkflowStub(
            $toJava.newUntypedWorkflowStub($workflowId, $runId.asJava, Option.empty[String].asJava)
          )
        )
      }
    }
    println(result.show)
    result
  }
}
