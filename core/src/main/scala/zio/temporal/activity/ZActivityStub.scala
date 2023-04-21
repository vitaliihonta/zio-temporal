package zio.temporal.activity

import io.temporal.workflow.ActivityStub
import zio.temporal.internal.ClassTagUtils
import zio.temporal.internalApi
import zio.temporal.internal.Stubs
import zio.temporal.workflow.ZAsync
import scala.reflect.ClassTag

sealed trait ZActivityStub {
  def toJava: ActivityStub

  def untyped: ZActivityStub.Untyped
}

final class ZActivityStubImpl @internalApi() (val toJava: ActivityStub) extends ZActivityStub {
  override val untyped: ZActivityStub.Untyped = new ZActivityStub.UntypedImpl(toJava)
}

object ZActivityStub extends Stubs[ZActivityStub] with ZActivityExecutionSyntax {

  /** An untyped version of [[ZActivityStub]]
    */
  sealed trait Untyped {
    def toJava: ActivityStub

    /** Executes an activity by its type name and arguments. Blocks until the activity completion.
      *
      * @tparam R
      *   return type.
      * @param activityName
      *   name of an activity type to execute.
      * @param args
      *   arguments of the activity.
      * @return
      *   an activity result.
      */
    def execute[R: ClassTag](activityName: String, args: Any*): R

    /** Executes an activity asynchronously by its type name and arguments.
      *
      * @tparam R
      *   return type.
      * @param activityName
      *   name of an activity type to execute.
      * @param args
      *   arguments of the activity.
      * @return
      *   Promise to the activity result.
      */
    def executeAsync[R: ClassTag](activityName: String, args: Any*): ZAsync[R]
  }

  private[temporal] class UntypedImpl(val toJava: ActivityStub) extends Untyped {
    override def execute[R: ClassTag](activityName: String, args: Any*): R =
      toJava.execute[R](activityName, ClassTagUtils.classOf[R], args.asInstanceOf[Seq[AnyRef]]: _*)

    override def executeAsync[R: ClassTag](activityName: String, args: Any*): ZAsync[R] =
      ZAsync.fromJava(
        toJava.executeAsync[R](activityName, ClassTagUtils.classOf[R], args.asInstanceOf[Seq[AnyRef]]: _*)
      )
  }

  final implicit class Ops[A](private val self: ZActivityStub.Of[A]) extends AnyVal {}
}
