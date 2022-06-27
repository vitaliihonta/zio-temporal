package com.example.payments

import com.example.payments.impl._
import com.example.payments.workflows._
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import distage.ModuleDef
import distage.config.AppConfig
import izumi.logstage.api.IzLogger
import izumi.logstage.api.Log
import izumi.logstage.api.routing.StaticLogRouter
import izumi.logstage.sink.ConsoleSink
import logstage.LogIO
import logstage.LogRouter
import zio.UIO
import zio.temporal.distage._
import zio.temporal.proto.ScalapbDataConverter
import zio.temporal.workflow.ZWorkflowClientOptions

object PaymentWorker extends ZWorkerDef("payments") {
  registerActivity[PaymentActivity].from[PaymentActivityImpl]

  registerWorkflow[PaymentWorkflow].fromFactory((rootLogger: IzLogger) => () => new PaymentWorkflowImpl(rootLogger))
}

object ExampleModule extends ModuleDef {
  make[Config].from(ConfigFactory.load())
  make[AppConfig].from(AppConfig(_))

  make[IzLogger].from {
    val logger = IzLogger(LogRouter(threshold = Log.Level.Info, sink = ConsoleSink.SimpleConsoleSink))
    StaticLogRouter.instance.setup(logger.router)
    logger
  }

  make[LogIO[UIO]].from(LogIO.fromLogger[UIO](_: IzLogger))

  include(TemporalZioConfigModule)
  include(TemporalZioClientModule)
  include(TemporalZioWorkerModule)
  include(PaymentWorker)

  make[ZWorkflowClientOptions].fromValue(
    ZWorkflowClientOptions.default.withDataConverter(
      ScalapbDataConverter.makeAutoLoad()
    )
  )

  make[ExampleFlow]
}
