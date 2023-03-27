package zio.temporal

import zio.Cause

/** Base error type representing possible temporal interaction errors.
  *
  * @tparam E
  *   \- possible business error type
  */
sealed trait TemporalError[+E] {
  def getError: Option[E]
  def message: String
}

object TemporalError {
  def apply[E](error: E): TemporalError[E] =
    TemporalBusinessError(error)
}

/** Low-level temporal client error
  */
case class TemporalClientError(error: Throwable) extends TemporalError[Nothing] {

  override val getError: Option[Nothing] = None

  override def message: String = s"$error cause=${error.getCause}"

  override def toString: String =
    s"TemporalClientError($message)"
}

/** Your business error propagated through ZIO error channel or Either.Left
  *
  * @tparam E
  *   business error type
  * @param error
  *   the error value
  */
case class TemporalBusinessError[E](error: E) extends TemporalError[E] {
  override val getError: Option[E] = Some(error)

  override def message: String = error.toString
}
