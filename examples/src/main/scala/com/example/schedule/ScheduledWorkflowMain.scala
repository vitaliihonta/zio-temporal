package com.example.schedule

import zio._
import zio.logging.backend.SLF4J
import zio.temporal.workflow._
import zio.temporal.schedules._
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}

object ScheduledWorkflowMain extends ZIOAppDefault {
  val TaskQueue = "schedules"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[HelloWorkflowWithTimeImpl].fromClass

    val startSchedules = ZIO.serviceWithZIO[ZScheduleClient] { scheduleClient =>
      for {
        scheduleId <- Random.nextUUID
        workflowId <- Random.nextUUID
        stub = scheduleClient
                 .newScheduleStartWorkflowStub[HelloWorkflowWithTime]()
                 .withTaskQueue(TaskQueue)
                 .withWorkflowId(workflowId.toString)
                 .build

        now <- Clock.instant

        calendarSpec = ZScheduleSpec
                         .calendars(
                           calendar
                             .withSeconds(range())
                             .withMinutes(range(to = 59, by = 10))
                             .withHour(range(from = 1, to = 23, by = 2))
                             .withDayOfMonth(allMonthDays)
                             .withMonth(allMonths)
                             .withDayOfWeek(allWeekDays)
                             .withComment("Every odd hour, every 10 minutes during an hour")
                         )
                         .withStartAt(now.plusSeconds(60))

        intervalSpec = ZScheduleSpec
                         .intervals(every(1.hour))
                         .withSkip(
                           // skip weekends
                           calendar.withDayOfWeek(range(from = 6, to = 7))
                         )

        schedule = ZSchedule
                     .withAction {
                       ZScheduleStartWorkflowStub.start(
                         stub.printGreeting("Hello!")
                       )
                     }
                     .withSpec(
                       // Choose whatever spec you want
                       intervalSpec
                     )

        handle <- scheduleClient.createSchedule(
                    scheduleId.toString,
                    schedule
                  )

        description <- handle.describe
        _           <- ZIO.logInfo(s"Created schedule=$description")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkflowServiceStubs.setup()
      _ <- startSchedules
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
