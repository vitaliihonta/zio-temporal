package com.example.schedule

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.workflow._
import zio.temporal.schedules._
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}

// todo: make it work
object ScheduledWorkflowMain extends ZIOAppDefault {
  val TaskQueue = "schedules"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[HelloWorkflowWithTimeImpl].fromClass

    val startWorkflows = ZIO.serviceWithZIO[ZScheduleClient] { scheduleClient =>
      for {
        scheduleId <- Random.nextUUID
        workflowId <- Random.nextUUID
        stub = scheduleClient
                 .newScheduleStartWorkflowStub[HelloWorkflowWithTime]()
                 .withTaskQueue(TaskQueue)
                 .withWorkflowId(workflowId.toString)
                 .build

        now <- Clock.instant
        schedule = ZSchedule
                     .withAction {
                       ZScheduleStartWorkflowStub.start(
                         stub.printGreeting("Hello!")
                       )
                     }
                     .withSpec(
                       ZScheduleSpec.withStartAt(now)
                     )

        _ <- scheduleClient.createSchedule(
               scheduleId.toString,
               schedule
             )
      } yield ()
    }

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkflowServiceStubs.setup()
      _ <- startWorkflows
      // todo: not sure about the order
      _ <- ZWorkerFactory.serve

    } yield ()

    program.provideSome[Scope](
      ZWorkflowServiceStubsOptions.make,
      ZWorkflowClientOptions.make,
      ZWorkerFactoryOptions.make,
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make,
      ZWorkerFactory.make,
      // schedules
      ZScheduleClient.make,
      ZScheduleClientOptions.make
    )
  }
}
