package zio.temporal.workflow

import zio.temporal.internal.ZWorkflowMacro
import zio.temporal.TemporalClientError
import zio.temporal.TemporalError
import zio.temporal.TemporalIO
import zio.temporal.ZWorkflowExecution
import zio.temporal.promise.ZPromise

import scala.language.experimental.macros
import scala.language.implicitConversions

trait ZWorkflowExecutionSyntax {
  def start[A](f: A): TemporalIO[TemporalClientError, ZWorkflowExecution] =
    macro ZWorkflowMacro.startImpl[A]

  def execute[R](f: R): TemporalIO[TemporalClientError, R] =
    macro ZWorkflowMacro.executeImpl[R]

  def execute[E, R](f: Either[E, R]): TemporalIO[TemporalError[E], R] =
    macro ZWorkflowMacro.executeEitherImpl[E, R]

  def async[R](f: R): ZPromise[Nothing, R] =
    macro ZWorkflowMacro.asyncImpl[R]

  def async[E, R](f: Either[E, R]): ZPromise[E, R] =
    macro ZWorkflowMacro.asyncEitherImpl[E, R]
}
