package com.example.payments

import com.example.payments.impl.PaymentActivityImpl
import com.example.payments.service.PaymentService
import zio.*
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.logging.backend.SLF4J
import zio.temporal.activity.ZActivityOptions
import zio.temporal.protobuf.ProtobufDataConverter

object Main extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val program = for {
      _ <- ZWorkerFactory.setup
      _ <- ZWorkflowServiceStubs.setup()
      _ <- ZIO.serviceWithZIO[ExampleFlow](_.proceedPayment())
    } yield ()

    program
      .provideSome[Scope](
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
        ZWorkerFactory.make
      )
  }
}
