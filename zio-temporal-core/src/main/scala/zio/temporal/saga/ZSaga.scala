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
  * @tparam E
  *   error type
  * @tparam A
  *   value type
  */
sealed trait ZSaga[+E, +A] { self =>

  /** Runs this saga, returning either error or successful value. Compensations are automatically applied if error
    * occurs
    *
    * @param options
    *   ZSaga options
    * @return
    *   either successful value or error (with compensations executed)
    */
  final def run(options: ZSaga.Options = ZSaga.Options.default): Either[E, A] =
    ZSaga.runImpl(self)(options)

  def swap: ZSaga[A, E] = ZSaga.Swap(this)

  def map[B](f: A => B): ZSaga[E, B] =
    ZSaga.Bind[E, A, B](this, a => ZSaga.Succeed(f(a)))

  def as[B](value: B): ZSaga[E, B] =
    self.map(_ => value)

  def unit: ZSaga[E, Unit] =
    self.as(())

  def mapError[E2](f: E => E2): ZSaga[E2, A] =
    ZSaga.BindError[E, E2, A](this, e => ZSaga.Succeed(f(e)))

  def flatMap[E0 >: E, B](f: A => ZSaga[E0, B]): ZSaga[E0, B] =
    ZSaga.Bind[E0, A, B](this, f)

  def flatMapError[E2](f: E => ZSaga[Nothing, E2]): ZSaga[E2, A] =
    ZSaga.BindError[E, E2, A](this, f)

  def catchAll[E2, A0 >: A](f: E => ZSaga[E2, A0]): ZSaga[E2, A0] =
    ZSaga.CatchAll[E, E2, A0](this, f)

  def catchSome[E0 >: E, A0 >: A](pf: PartialFunction[E, ZSaga[E0, A0]]): ZSaga[E0, A0] =
    ZSaga.CatchAll[E, E0, A0](this, pf.applyOrElse(_, ZSaga.Failed[E0](_)))

  final def zipWith[E1 >: E, B, C](that: => ZSaga[E1, B])(f: (A, B) => C): ZSaga[E1, C] =
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
  def succeed[A](value: A): ZSaga[Nothing, A] =
    ZSaga.Succeed(value)

  /** Creates immediately failed [[ZSaga]] instance
    * @tparam E
    *   error type
    * @param error
    *   error value
    * @return
    *   failed [[ZSaga]]
    */
  def fail[E](error: E): ZSaga[E, Nothing] =
    ZSaga.Failed(error)

  /** Suspends side effect execution within [[ZSaga]]
    * @tparam E
    *   error value
    * @tparam A
    *   value type
    * @param thunk
    *   side effect (which should not throw exceptions)
    * @return
    *   suspended [[ZSaga]]
    */
  def fromEither[E, A](thunk: => Either[E, A]): ZSaga[E, A] =
    ZSaga.FromEither(() => thunk)

  /** Suspends side effect execution within [[ZSaga]]
    * @tparam A
    *   value type
    * @param thunk
    *   effectful side effect (which may throw exceptions)
    * @return
    *   suspended [[ZSaga]]
    */
  def effect[A](thunk: => A): ZSaga[Throwable, A] =
    fromEither(Try(thunk).toEither)

  def make[E, A](exec: => Either[E, A])(compensate: => Unit): ZSaga[E, A] =
    ZSaga.Compensation[E, A](() => compensate, ZSaga.FromEither(() => exec))

  def foreach[E, A, B](in: Option[A])(f: A => ZSaga[E, B]): ZSaga[E, Option[B]] =
    in.fold[ZSaga[E, Option[B]]](succeed(None))(f(_).map(Some(_)))

  def foreach[E, A, B, Collection[+Element] <: Iterable[Element]](
    in:          Collection[A]
  )(f:           A => ZSaga[E, B]
  )(implicit bf: BuildFrom[Collection[A], B, Collection[B]]
  ): ZSaga[E, Collection[B]] =
    in.foldLeft[ZSaga[E, mutable.Builder[B, Collection[B]]]](succeed(bf(in)))((io, a) => io.zipWith(f(a))(_ += _))
      .map(_.result())

  final case class FromEither[E, A] private[zio] (apply: () => Either[E, A]) extends ZSaga[Nothing, A]

  final case class Swap[E, A] private[zio] (base: ZSaga[E, A]) extends ZSaga[A, E] {
    override def swap: ZSaga[E, A] = base
  }

  final case class Succeed[A] private[zio] (value: A) extends ZSaga[Nothing, A] {
    override def swap: ZSaga[A, Nothing] = ZSaga.Failed(value)

    override def map[B](f: A => B): ZSaga[Nothing, B] = ZSaga.Succeed(f(value))

    override def flatMap[E0 >: Nothing, B](f: A => ZSaga[E0, B]): ZSaga[E0, B] =
      f(value)

    override def mapError[E2](f: Nothing => E2): ZSaga[E2, A] = this

    override def flatMapError[E2](f: Nothing => ZSaga[Nothing, E2]): ZSaga[E2, A] = this

    override def catchAll[E2, A0 >: A](f: Nothing => ZSaga[E2, A0]): ZSaga[E2, A0] = this

    override def catchSome[E0 >: Nothing, A0 >: A](pf: PartialFunction[Nothing, ZSaga[E0, A0]]): ZSaga[E0, A0] = this
  }

  final case class Failed[E] private[zio] (error: E) extends ZSaga[E, Nothing] {
    override def swap: ZSaga[Nothing, E] = ZSaga.Succeed(error)

    override def mapError[E2](f: E => E2): ZSaga[E2, Nothing] = ZSaga.Failed(f(error))

    override def flatMapError[E2](f: E => ZSaga[Nothing, E2]): ZSaga[E2, Nothing] = f(error).swap

    override def catchAll[E2, A0 >: Nothing](f: E => ZSaga[E2, A0]): ZSaga[E2, A0] = f(error)

    override def catchSome[E0 >: E, A0 >: Nothing](pf: PartialFunction[E, ZSaga[E0, A0]]): ZSaga[E0, A0] =
      pf.applyOrElse[E, ZSaga[E0, A0]](error, _ => this)
  }

  final case class Compensation[E, A] private[zio] (compensate: () => Unit, cont: ZSaga[E, A])
      extends ZSaga[E, A] {
    override def swap: ZSaga[A, E] = ZSaga.Compensation(compensate, cont.swap)

    override def map[B](f: A => B): ZSaga[E, B] = ZSaga.Compensation(compensate, cont.map(f))

    override def mapError[E2](f: E => E2): ZSaga[E2, A] = ZSaga.Compensation(compensate, cont.mapError(f))
  }

  final case class Bind[E, A, B] private[zio] (base: ZSaga[E, A], cont: A => ZSaga[E, B]) extends ZSaga[E, B] {
    override def map[B2](f: B => B2): ZSaga[E, B2] = ZSaga.Bind[E, A, B2](base, cont(_).map(f))

    override def flatMap[E0 >: E, B2](f: B => ZSaga[E0, B2]): ZSaga[E0, B2] =
      ZSaga.Bind[E0, A, B2](base, cont(_).flatMap(f))
  }

  final case class BindError[E0, E, A] private[zio] (base: ZSaga[E0, A], cont: E0 => ZSaga[Nothing, E])
      extends ZSaga[E, A] {

    override def mapError[E2](f: E => E2): ZSaga[E2, A] =
      ZSaga.BindError[E0, E2, A](base, cont(_).map(f))
  }

  final case class CatchAll[E0, E, A] private[zio] (base: ZSaga[E0, A], handle: E0 => ZSaga[E, A])
      extends ZSaga[E, A] {

    override def catchAll[E2, A0 >: A](f: E => ZSaga[E2, A0]): ZSaga[E2, A0] =
      ZSaga.CatchAll[E0, E2, A0](base, handle(_).catchAll(f))
  }

  private[zio] def runImpl[E, A](self: ZSaga[E, A])(options: ZSaga.Options): Either[E, A] = {
    val temporalSagaOptions = new Saga.Options.Builder()
      .setParallelCompensation(options.parallelCompensation)
      .setContinueWithError(options.continueWithError)
      .build()

    val temporalSaga = new Saga(temporalSagaOptions)

    def interpret[E0, A0](saga: ZSaga[E0, A0]): Either[E0, A0] =
      saga match {
        case succeed: Succeed[A0]     => Right(succeed.value)
        case failed: Failed[E0]       => Left(failed.error)
        case exec: FromEither[E0, A0] => exec.apply()

        case swap: Swap[A0, E0] => interpret(swap.base).swap

        case bindError: BindError[baseE, E0, A0] =>
          interpret(bindError.base) match {
            case right: Right[_, A0] => right.asInstanceOf[Either[E0, A0]]
            case Left(error) =>
              interpret[Nothing, E0](bindError.cont(error)).swap
          }

        case compensation: Compensation[E0, A0] =>
          temporalSaga.addCompensation((() => compensation.compensate()): Proc)
          interpret(compensation.cont)

        case cont: Bind[E0, baseA, A0] =>
          interpret(cont.base) match {
            case left @ Left(_) => left.asInstanceOf[Either[E0, A0]]
            case Right(value)   => interpret(cont.cont(value))
          }

        case catchAll: CatchAll[baseE, E0, A0] =>
          interpret(catchAll.base) match {
            case right: Right[E0, A0] => right
            case Left(error)          => interpret(catchAll.handle(error))
          }
      }

    val result = interpret(self)

    result.left.foreach(_ => temporalSaga.compensate())

    result
  }
}
