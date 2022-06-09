package com.example.payments

import distage.ModuleDef
import ztemporal.distage._
import com.example.payments.workflows._
import com.example.payments.impl._
import com.example.transactions.TransactionsProto
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import distage.config.AppConfig
import izumi.logstage.api.IzLogger
import izumi.logstage.api.Log
import izumi.logstage.api.routing.StaticLogRouter
import izumi.logstage.sink.ConsoleSink
import logstage.LogIO
import logstage.LogRouter
import zio.UIO
import ztemporal.distage.ZWorkerDef
import ztemporal.proto.ScalapbDataConverter
import ztemporal.workflow.ZWorkflowClientOptions

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

  include(ZTemporalConfigModule)
  include(ZTemporalClientModule)
  include(ZTemporalWorkerModule)
  include(PaymentWorker)

  make[ZWorkflowClientOptions].fromValue(
    ZWorkflowClientOptions.default.withDataConverter(
      ScalapbDataConverter.make(
        List(
          TransactionsProto
        )
      )
    )
  )

  make[ExampleFlow]
}
