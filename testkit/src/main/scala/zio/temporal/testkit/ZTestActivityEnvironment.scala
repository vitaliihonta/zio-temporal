package zio.temporal.testkit

import io.temporal.testing.TestActivityEnvironment
import zio.temporal.activity.{ExtendsActivity, IsActivity, ZActivityRunOptions, ZActivityStubBuilderInitial}
import zio._
import zio.temporal.{JavaTypeTag, TypeIsSpecified}
import zio.temporal.internal.ClassTagUtils

import scala.reflect.ClassTag

class ZTestActivityEnvironment[+R] private[zio] (
  val toJava: TestActivityEnvironment,
  runtime:    zio.Runtime[R]) {

  implicit lazy val activityOptions: ZActivityRunOptions[R] =
    new ZActivityRunOptions[R](runtime, None)

  /** Registers activity implementations to test. Use [[newActivityStub]] to create stubs that can be used to invoke
    * them.
    *
    * <p>Implementations that share a worker must implement different interfaces as an activity type is identified by
    * the activity interface, not by the implementation.
    *
    * @throws TypeAlreadyRegisteredException
    *   if one of the activity types is already registered
    */
  def addActivityImplementation[A <: AnyRef: ExtendsActivity](activity: A): UIO[Unit] =
    ZIO.succeed {
      toJava.registerActivitiesImplementations(activity)
    }

  /** Registers activity implementations to test. Use [[newActivityStub]] to create stubs that can be used to invoke
    * them.
    *
    * <p>Implementations that share a worker must implement different interfaces as an activity type is identified by
    * the activity interface, not by the implementation.
    *
    * @throws TypeAlreadyRegisteredException
    *   if one of the activity types is already registered
    */
  def addActivityImplementationService[A <: AnyRef: ExtendsActivity: Tag]: URIO[A, Unit] = {
    ZIO.serviceWithZIO[A] { activity =>
      addActivityImplementation[A](activity)
    }
  }

  /** Creates a stub that can be used to invoke activities registered through [[addActivityImplementation]]
    *
    * @note
    *   it's not a [[zio.temporal.activity.ZActivityStub]] because the activity is invoked locally. Wrapping method
    *   invocation into [[zio.temporal.activity.ZActivityStub.execute]] is not required
    * @tparam A
    *   Type of the activity interface.
    * @return
    *   The stub builder for the activity.
    */
  def newActivityStub[A <: AnyRef: IsActivity: ClassTag]: ZActivityStubBuilderInitial[UIO[A]] =
    new ZActivityStubBuilderInitial[UIO[A]](
      buildImpl = options => ZIO.succeed(toJava.newActivityStub(ClassTagUtils.classOf[A], options))
    )

  /** Sets a listener that is called every time an activity implementation heartbeats through
    * [[zio.temporal.activity.ZActivityExecutionContext.heartbeat]]
    *
    * @tparam T
    *   type of the details passed to the [[zio.temporal.activity.ZActivityExecutionContext.heartbeat]]
    * @param listener
    *   listener to register.
    */
  def setActivityHeartbeatListener[T: TypeIsSpecified: JavaTypeTag](listener: T => Unit): UIO[Unit] =
    ZIO.succeed(
      toJava.setActivityHeartbeatListener[T](
        JavaTypeTag[T].klass,
        JavaTypeTag[T].genericType,
        (heartbeat: T) => listener(heartbeat)
      )
    )

  /** Sets heartbeat details for the next activity execution. The next activity called from this TestActivityEnvironment
    * will be able to access this value using [[zio.temporal.activity.ZActivityExecutionContext.heartbeat]]. This value
    * is cleared upon execution.
    *
    * @tparam T
    *   Type of the heartbeat details.
    *
    * @param details
    *   The details object to make available to the next activity call.
    */
  def setHeartbeatDetails[T](details: T): UIO[Unit] =
    ZIO.succeed(toJava.setHeartbeatDetails(details))

  /** Requests activity cancellation. The cancellation is going to be delivered to the activity on the next heartbeat.
    */
  def requestCancelActivity(): UIO[Unit] =
    ZIO.succeed(toJava.requestCancelActivity())
}

object ZTestActivityEnvironment {
  def activityOptions[R: Tag]: URIO[ZTestActivityEnvironment[R], ZActivityRunOptions[R]] =
    ZIO.serviceWith[ZTestActivityEnvironment[R]](_.activityOptions)

  /** Registers activity implementations to test. Use [[newActivityStub]] to create stubs that can be used to invoke
    * them.
    *
    * <p>Implementations that share a worker must implement different interfaces as an activity type is identified by
    * the activity interface, not by the implementation.
    *
    * @throws TypeAlreadyRegisteredException
    *   if one of the activity types is already registered
    */
  def addActivityImplementation[A <: AnyRef: ExtendsActivity](activity: A): URIO[ZTestActivityEnvironment[Any], Unit] =
    ZIO.serviceWithZIO[ZTestActivityEnvironment[Any]](_.addActivityImplementation(activity))

  /** Registers activity implementations to test. Use [[newActivityStub]] to create stubs that can be used to invoke
    * them.
    *
    * <p>Implementations that share a worker must implement different interfaces as an activity type is identified by
    * the activity interface, not by the implementation.
    *
    * @throws TypeAlreadyRegisteredException
    *   if one of the activity types is already registered
    */
  def addActivityImplementationService[
    A <: AnyRef: ExtendsActivity: Tag
  ]: URIO[ZTestActivityEnvironment[Any] with A, Unit] =
    ZIO.serviceWithZIO[ZTestActivityEnvironment[Any]](_.addActivityImplementationService)

  /** Creates a stub that can be used to invoke activities registered through [[addActivityImplementation]]
    *
    * @note
    *   it's not a [[zio.temporal.activity.ZActivityStub]] because the activity is invoked locally. Wrapping method
    *   invocation into [[zio.temporal.activity.ZActivityStub.execute]] is not required
    * @tparam A
    *   Type of the activity interface.
    * @return
    *   The stub builder for the activity.
    */
  def newActivityStub[
    A <: AnyRef: IsActivity: ClassTag
  ]: ZActivityStubBuilderInitial[URIO[ZTestActivityEnvironment[Any], A]] =
    new ZActivityStubBuilderInitial[URIO[ZTestActivityEnvironment[Any], A]](
      buildImpl = options =>
        ZIO.serviceWithZIO[ZTestActivityEnvironment[Any]] { testEnv =>
          ZIO.succeed(testEnv.toJava.newActivityStub(ClassTagUtils.classOf[A], options))
        }
    )

  /** Sets a listener that is called every time an activity implementation heartbeats through
    * [[zio.temporal.activity.ZActivityExecutionContext.heartbeat]]
    *
    * @tparam T
    *   type of the details passed to the [[zio.temporal.activity.ZActivityExecutionContext.heartbeat]]
    * @param listener
    *   listener to register.
    */
  def setActivityHeartbeatListener[T: JavaTypeTag](listener: T => Unit): URIO[ZTestActivityEnvironment[Any], Unit] =
    ZIO.serviceWithZIO[ZTestActivityEnvironment[Any]](_.setActivityHeartbeatListener(listener))

  /** Sets heartbeat details for the next activity execution. The next activity called from this TestActivityEnvironment
    * will be able to access this value using [[zio.temporal.activity.ZActivityExecutionContext.heartbeat]]. This value
    * is cleared upon execution.
    *
    * @tparam T
    *   Type of the heartbeat details.
    * @param details
    *   The details object to make available to the next activity call.
    */
  def setHeartbeatDetails[T](details: T): URIO[ZTestActivityEnvironment[Any], Unit] =
    ZIO.serviceWithZIO[ZTestActivityEnvironment[Any]](_.setHeartbeatDetails(details))

  /** Requests activity cancellation. The cancellation is going to be delivered to the activity on the next heartbeat.
    */
  def requestCancelActivity(): URIO[ZTestActivityEnvironment[Any], Unit] =
    ZIO.serviceWithZIO[ZTestActivityEnvironment[Any]](_.requestCancelActivity())

  /** Creates a new instance of [[ZTestActivityEnvironment]]
    *
    * @see
    *   [[TestActivityEnvironment.newInstance]]
    * @return
    *   managed instance of test activity environment
    */
  def make[R: Tag]: URLayer[R with ZTestEnvironmentOptions, ZTestActivityEnvironment[R]] =
    ZLayer.scoped[R with ZTestEnvironmentOptions] {
      for {
        runtime <- ZIO.runtime[R with ZTestEnvironmentOptions]
        env <- ZIO.succeedBlocking(
                 new ZTestActivityEnvironment[R](
                   TestActivityEnvironment.newInstance(runtime.environment.get[ZTestEnvironmentOptions].toJava),
                   runtime
                 )
               )
        _ <- ZIO.addFinalizer(
               ZIO.attempt(env.toJava.close()).ignore
             )
      } yield env
    }

  /** Creates a new instance of [[ZTestActivityEnvironment]] with default options
    *
    * @return
    *   managed instance of test activity environment
    */
  def makeDefault[R: Tag]: URLayer[R, ZTestActivityEnvironment[Any]] =
    ZLayer.makeSome[R, ZTestActivityEnvironment[R]](
      ZTestEnvironmentOptions.default,
      make[R]
    )
}
