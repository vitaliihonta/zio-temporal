package com.example.polling.periodic

import zio._
class TestServiceException(msg: String) extends Exception(msg)

object TestService {
  private val errorAttemptsConfig = Config.int("error-attempts").withDefault(5)

  val make: Layer[Config.Error, TestService] = {
    ZLayer.fromZIO {
      for {
        errorAttempts  <- ZIO.config(errorAttemptsConfig)
        tryAttemptsRef <- Ref.make(0)
        _              <- ZIO.logInfo(s"Error attempts: $errorAttempts")
      } yield new TestService(tryAttemptsRef, errorAttempts)
    }
  }
}

class TestService private (tryAttemptsRef: Ref[Int], errorAttempts: Int) {
  def getServiceResult: IO[TestServiceException, String] = {
    for {
      tryAttempts <- tryAttemptsRef.updateAndGet(_ + 1)
      _           <- ZIO.logInfo(s"Attempt try #$tryAttempts")
      _ <- ZIO.when(tryAttempts % errorAttempts != 0) {
             ZIO.fail(new TestServiceException("Service is down"))
           }
    } yield "OK"
  }
}
