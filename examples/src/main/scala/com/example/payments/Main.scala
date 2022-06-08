package com.example.payments

import distage.DIKey
import distage.Injector
import distage.Roots
import logstage.LogIO
import zio._
import ztemporal.distage.ZWorkerLifecycle

object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    Injector[RIO[ZEnv, *]]()
      .produce(ExampleModule, Roots(DIKey[ZWorkerLifecycle], DIKey[ExampleFlow]))
      .use { locator =>
        val logger          = locator.get[LogIO[UIO]]
        val workerLifecycle = locator.get[ZWorkerLifecycle]
        logger.info(s"Going to start temporal workers ${workerLifecycle.info}")
        workerLifecycle.withWorkersStarted.use { _ =>
          locator.get[ExampleFlow].proceedPayment()
        }
      }
      .exitCode
}
