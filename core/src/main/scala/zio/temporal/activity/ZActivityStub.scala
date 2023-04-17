package zio.temporal.activity

import io.temporal.workflow.ActivityStub
import zio.temporal.internalApi
import zio.temporal.internal.tagging.Stubs

sealed trait ZActivityStub {
  def toJava: ActivityStub
}

class ZActivityStubImpl @internalApi() (val toJava: ActivityStub) extends ZActivityStub {}

object ZActivityStub extends Stubs[ZActivityStub] with ZActivityExecutionSyntax {
  final implicit class Ops[A](private val self: ZActivityStub.Of[A]) extends AnyVal {}
}
