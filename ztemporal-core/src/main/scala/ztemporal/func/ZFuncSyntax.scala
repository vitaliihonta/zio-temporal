package ztemporal.func

trait ZFuncSyntax {

  @inline final implicit def ZWorkflowFunc0[A](f: () => A): ZFunc0[A] = new ZFunc0(f)

  @inline final implicit def ZWorkflowFunc1[A, B](f: A => B): ZFunc1[A, B] = new ZFunc1(f)

  @inline final implicit def ZWorkflowFunc2[A, B, C](f: (A, B) => C): ZFunc2[A, B, C] = new ZFunc2(f)
}
