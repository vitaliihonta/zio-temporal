package ztemporal

import zio.Cause

/** Base error type representing possible temporal interaction errors.
  *
  * @tparam E - possible business error type
  */
sealed trait ZTemporalError[+E]

/** Low-level temporal client error
  */
case class ZTemporalClientError(error: Throwable) extends ZTemporalError[Nothing] {

  override def toString: String = {
    val message = s"$error cause=${error.getCause}"
    s"ZTemporalClientError($message)"
  }
}

/** Your business error propagated through ZIO error channel or Either.Left
  *
  * @tparam E business error type
  * @param error the error value
  */
case class ZTemporalBusinessError[E](error: E) extends ZTemporalError[E]

/** Represents fatal errors that may appear in your business logic.
  *
  *  It's thrown when [[zio.ZIO.die]] and co. was triggered
  *  @param error captured [[Cause]]
  */
case class ZActivityFatalError(error: Cause[_]) extends RuntimeException {

  override def getMessage: String = {
    val failure = error.dieOption.map(_.toString).getOrElse("unknown")
    s"Fatal error running temporal activity: $failure"
  }
}
