package com.example.payments

import com.example.payments.impl.PaymentActivityImpl
import com.example.payments.service.PaymentService
import zio._
import zio.temporal.worker.ZWorkerFactory
import zio.temporal.workflow.ZWorkflowClient
import zio.temporal.workflow.ZWorkflowServiceStubs
import zio.logging.backend.SLF4J
import zio.temporal.activity.ZActivityOptions

object Main extends ZIOAppDefault {
  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program =
      ZIO.service[ZWorkerFactory].flatMap { workerFactory =>
        workerFactory.use {
          for {
            flow  <- ZIO.service[ExampleFlow]
            stubs <- ZIO.service[ZWorkflowServiceStubs]
            _ <- stubs.use() {
                   flow.proceedPayment()
                 }
          } yield ()
        }
      }

    program
      .provide(
        ExampleModule.clientOptions,
        ExampleModule.stubOptions,
        ExampleModule.workerFactoryOptions,
        ExampleModule.worker,
        ZWorkflowClient.make,
        PaymentActivityImpl.make,
        PaymentService.make,
        ExampleFlow.make,
        ZActivityOptions.default,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
        SLF4J.slf4j
      )
  }
}
