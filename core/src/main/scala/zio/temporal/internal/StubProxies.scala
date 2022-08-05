package zio.temporal.internal

import scala.reflect.ClassTag

private[zio] object StubProxies {
  def proxy[Delegate, Proxied](
    delegate:             Delegate
  )(implicit delegateCtg: ClassTag[Delegate],
    proxiedCtg:           ClassTag[Proxied]
  ): Delegate & Proxied = {
    val Delegate = delegateCtg.runtimeClass
    val Proxied  = proxiedCtg.runtimeClass
    val proxy = java.lang.reflect.Proxy.newProxyInstance(
      getClass.getClassLoader,
      Array(Delegate, Proxied),
      (proxy, method, methodArgs) =>
        if (method.getDeclaringClass.isAssignableFrom(Delegate)) {
          println(s"Delegating $method call to delegate")
          method.invoke(delegate, methodArgs: _*)
        } else {
          null
//          throw new IllegalStateException(
//            s"Proxied methods of $Proxied should not be invoked at runtime!\n" +
//              s"But invoked $method"
//          )
        }
    )

    proxy.asInstanceOf[Delegate & Proxied]
  }
}
