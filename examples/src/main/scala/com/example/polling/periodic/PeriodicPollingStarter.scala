package com.example.polling.periodic

import zio.*
import zio.temporal.*
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker.*
import zio.temporal.workflow.*
import zio.logging.backend.SLF4J

object PeriodicPollingStarter extends ZIOAppDefault {
  val TaskQueue = "period-polling-workflows"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[PeriodicPollingChildWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[PeriodicPollingWorkflowImpl].fromClass @@
        ZWorker.addActivityImplementationService[PollingActivities]

    val invokeWorkflows = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        workflowId <- ZIO.randomWith(_.nextUUID)
        workflow <- client
                      .newWorkflowStub[PollingWorkflow]
                      .withTaskQueue(TaskQueue)
                      .withWorkflowId(workflowId.toString)
                      .build
        result <- ZWorkflowStub.execute(
                    workflow.exec()
                  )
        _ <- ZIO.logInfo(s"Result: $result")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkerFactory.setup
      _ <- ZWorkflowServiceStubs.setup()
      _ <- invokeWorkflows
    } yield ()

    val configMap = Map[String, String](
      // change error attempts in case you need it
      "error-attempts" -> "12"
    )

    program
      .provideSome[Scope](
        ZLayer.succeed(ZWorkflowServiceStubsOptions.default),
        ZLayer.succeed(ZWorkflowClientOptions.default),
        ZLayer.succeed(ZWorkerFactoryOptions.default),
        // service layers
        TestService.make,
        PeriodicPollingActivityImpl.make,
        ZActivityOptions.default,
        //
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
      .withConfigProvider(
        ConfigProvider.fromMap(configMap)
      )
  }
}
