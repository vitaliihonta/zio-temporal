package com.example.payments

import com.example.transactions.*
import com.example.payments.impl.*
import com.example.payments.workflows.*
import zio.*
import zio.temporal.*
import zio.temporal.protobuf.ProtobufDataConverter
import zio.temporal.worker.ZWorker
import zio.temporal.worker.ZWorkerFactory
import zio.temporal.worker.ZWorkerFactoryOptions
import zio.temporal.workflow.{
  ZWorkflowClient,
  ZWorkflowClientOptions,
  ZWorkflowServiceStubs,
  ZWorkflowServiceStubsOptions
}

object ExampleModule {
  val stubOptions: Layer[Config.Error, ZWorkflowServiceStubsOptions] = ZWorkflowServiceStubsOptions.make

  val workerFactoryOptions: Layer[Config.Error, ZWorkerFactoryOptions] = ZWorkerFactoryOptions.make @@
    ZWorkerFactoryOptions.withEnableLoggingInReplay(true)

  val clientOptions: Layer[Config.Error, ZWorkflowClientOptions] =
    ZWorkflowClientOptions.make @@
      ZWorkflowClientOptions.withDataConverter(ProtobufDataConverter.makeAutoLoad())

  val worker: URLayer[PaymentActivity with ZWorkerFactory, Unit] =
    ZLayer.fromZIO {
      ZWorkerFactory.newWorker("payments") @@
        ZWorker.addActivityImplementationService[PaymentActivity] @@
        ZWorker.addWorkflow[PaymentWorkflow].from(new PaymentWorkflowImpl)
    }.unit
}
