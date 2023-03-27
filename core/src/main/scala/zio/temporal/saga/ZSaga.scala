package zio.temporal.saga

import io.temporal.workflow.Functions.Proc
import io.temporal.workflow.Saga
import zio.BuildFrom

import scala.collection.mutable
import scala.util.Try

/** Implements the logic to execute compensation operations that is often required in Saga applications. The following
  * is a skeleton to show of how it is supposed to be used in workflow code:
  *
  * @see
  *   https://en.wikipedia.org/wiki/Compensating_transaction
  * @tparam A
  *   value type
  */
sealed trait ZSaga[+A] { self =>

  /** Runs this saga, returning either error or successful value. Compensations are automatically applied if error
    * occurs
    *
    * @param options
    *   ZSaga options
    * @return
    *   either successful value or error (with compensations executed)
    */
  final def run(options: ZSaga.Options = ZSaga.Options.default): Either[Throwable, A] =
    ZSaga.runImpl(self)(options)

  final def runOrThrow(options: ZSaga.Options = ZSaga.Options.default): A =
    run(options) match {
      case Right(value) => value
      case Left(error)  => throw error
    }

  def map[B](f: A => B): ZSaga[B] =
    ZSaga.Bind[A, B](this, a => ZSaga.Succeed(f(a)))

  def as[B](value: B): ZSaga[B] =
    self.map(_ => value)

  def unit: ZSaga[Unit] =
    self.as(())
  def flatMap[B](f: A => ZSaga[B]): ZSaga[B] =
    ZSaga.Bind[A, B](this, f)

  def catchAll[A0 >: A](f: Throwable => ZSaga[A0]): ZSaga[A0] =
    ZSaga.CatchAll[A0](this, f)

  def catchSome[A0 >: A](pf: PartialFunction[Throwable, ZSaga[A0]]): ZSaga[A0] =
    ZSaga.CatchAll[A0](this, pf.applyOrElse(_, ZSaga.Failed(_)))

  final def zipWith[B, C](that: => ZSaga[B])(f: (A, B) => C): ZSaga[C] =
    self.flatMap(a => that.map(f(a, _)))
}

object ZSaga {
  final case class Options(parallelCompensation: Boolean = false, continueWithError: Boolean = false)

  object Options {
    val default: Options = Options()
  }

  /** Creates immediately completed [[ZSaga]] instance which won't fail
    * @tparam A
    *   value type
    * @param value
    *   result value
    * @return
    *   value wrapped into [[ZSaga]]
    */
  def succeed[A](value: A): ZSaga[A] =
    ZSaga.Succeed(value)

  /** Creates a completed [[ZSaga]] with [[Unit]] result.
    */
  val unit: ZSaga[Unit] =
    succeed(())

  /** Creates immediately failed [[ZSaga]] instance
    * @param error
    *   error value
    * @return
    *   failed [[ZSaga]]
    */
  def fail(error: Throwable): ZSaga[Nothing] =
    ZSaga.Failed(error)

  /** Suspends side effect execution within [[ZSaga]]
    * @tparam A
    *   value type
    * @param thunk
    *   effectful side effect (which may throw exceptions)
    * @return
    *   suspended [[ZSaga]]
    */
  def attempt[A](thunk: => A): ZSaga[A] =
    ZSaga.Attempt(() => thunk)

  /** Creates a saga which will run a compensation if the main action fails.
    *
    * @tparam E
    *   typed error
    * @tparam A
    *   action result
    * @param exec
    *   the main action
    * @param compensate
    *   the compensation which will run in case the main action returns Left
    */
  def make[A](exec: => A)(compensate: => Unit): ZSaga[A] =
    ZSaga.Compensation[A](() => compensate, ZSaga.Attempt(() => exec))

  def foreach[A, B](in: Option[A])(f: A => ZSaga[B]): ZSaga[Option[B]] =
    in.fold[ZSaga[Option[B]]](succeed(None))(f(_).map(Some(_)))

  def foreach[A, B, Collection[+Element] <: Iterable[Element]](
    in:          Collection[A]
  )(f:           A => ZSaga[B]
  )(implicit bf: BuildFrom[Collection[A], B, Collection[B]]
  ): ZSaga[Collection[B]] =
    in.foldLeft[ZSaga[mutable.Builder[B, Collection[B]]]](succeed(bf(in)))((io, a) => io.zipWith(f(a))(_ += _))
      .map(_.result())

  final case class Attempt[A] private[zio] (thunk: () => A) extends ZSaga[A]

  final case class Succeed[A] private[zio] (value: A) extends ZSaga[A] {

    override def map[B](f: A => B): ZSaga[B] = ZSaga.Succeed(f(value))

    override def flatMap[B](f: A => ZSaga[B]): ZSaga[B] =
      f(value)

    override def catchAll[A0 >: A](f: Throwable => ZSaga[A0]): ZSaga[A0] = this

    override def catchSome[A0 >: A](pf: PartialFunction[Throwable, ZSaga[A0]]): ZSaga[A0] = this
  }

  final case class Failed private[zio] (error: Throwable) extends ZSaga[Nothing] {

    override def catchAll[A0 >: Nothing](f: Throwable => ZSaga[A0]): ZSaga[A0] = f(error)

    override def catchSome[A0 >: Nothing](pf: PartialFunction[Throwable, ZSaga[A0]]): ZSaga[A0] =
      pf.applyOrElse[Throwable, ZSaga[A0]](error, _ => this)
  }

  final case class Compensation[A] private[zio] (compensate: () => Unit, cont: ZSaga[A]) extends ZSaga[A] {

    override def map[B](f: A => B): ZSaga[B] = ZSaga.Compensation(compensate, cont.map(f))
  }

  final case class Bind[A, B] private[zio] (base: ZSaga[A], cont: A => ZSaga[B]) extends ZSaga[B] {
    override def map[B2](f: B => B2): ZSaga[B2] = ZSaga.Bind[A, B2](base, cont(_).map(f))

    override def flatMap[B2](f: B => ZSaga[B2]): ZSaga[B2] =
      ZSaga.Bind[A, B2](base, cont(_).flatMap(f))
  }

  final case class CatchAll[A] private[zio] (base: ZSaga[A], handle: Throwable => ZSaga[A]) extends ZSaga[A] {

    override def catchAll[A0 >: A](f: Throwable => ZSaga[A0]): ZSaga[A0] =
      ZSaga.CatchAll[A0](base, handle(_).catchAll(f))
  }

  private[zio] def runImpl[E, A](self: ZSaga[A])(options: ZSaga.Options): Either[Throwable, A] = {
    val temporalSagaOptions = new Saga.Options.Builder()
      .setParallelCompensation(options.parallelCompensation)
      .setContinueWithError(options.continueWithError)
      .build()

    val temporalSaga = new Saga(temporalSagaOptions)

    def interpret[E0, A0](saga: ZSaga[A0]): Either[Throwable, A0] =
      saga match {
        case succeed: Succeed[A0] => Right(succeed.value)
        case failed: Failed       => Left(failed.error)
        case attempt: Attempt[A0] => Try(attempt.thunk()).toEither

        case compensation: Compensation[A0] =>
          temporalSaga.addCompensation((() => compensation.compensate()): Proc)
          interpret(compensation.cont)

        case cont: Bind[baseA, A0] =>
          interpret(cont.base) match {
            case left @ Left(_) => left.asInstanceOf[Either[Throwable, A0]]
            case Right(value)   => interpret(cont.cont(value))
          }

        case catchAll: CatchAll[A0] =>
          interpret(catchAll.base) match {
            case right: Right[Throwable, A0] => right
            case Left(error)                 => interpret(catchAll.handle(error))
          }
      }

    val result = interpret(self)

    result.left.foreach(_ => temporalSaga.compensate())

    result
  }
}
