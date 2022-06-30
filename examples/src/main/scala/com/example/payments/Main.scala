package com.example.payments

import com.example.payments.impl.PaymentActivityImpl
import zio._
import zio.temporal.worker.ZWorkerFactory
import zio.temporal.worker.ZWorkerFactoryOptions
import zio.temporal.workflow.ZWorkflowClient
import zio.temporal.workflow.ZWorkflowServiceStubs
import zio.logging.backend.SLF4J
import zio.temporal.activity.ZActivityOptions

object Main extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      workerFactory <- ZIO.service[ZWorkerFactory]
      startFiber    <- workerFactory.start.fork
      flow          <- ZIO.service[ExampleFlow]
      _ <- ZIO
             .serviceWithZIO[ZWorkflowServiceStubs] { stubs =>
               stubs.use() {
                 flow.proceedPayment()
               }
             }
      _ <- startFiber.join *> workerFactory.shutdownNow
    } yield ()

    program
      .provide(
        ExampleModule.clientOptions,
        ExampleModule.stubOptions,
        ExampleModule.workerFactoryOptions,
        ExampleModule.worker,
        ZWorkflowClient.make,
        PaymentActivityImpl.make,
        ExampleFlow.make,
        ZActivityOptions.default,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
        SLF4J.slf4j(LogLevel.Debug)
      )
  }
}
