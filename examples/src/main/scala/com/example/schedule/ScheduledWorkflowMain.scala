package com.example.schedule

import io.temporal.client.schedules.ScheduleIntervalSpec
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
        // todo: figure out how this hugging api works
        myCalendar = calendar
                       .withSeconds(range())
                       .withMinutes(range(by = 5))
                       .withHour(range(from = 10, to = 14))
                       .withDayOfMonth(allMonthDays)
                       .withMonth(allMonths)
                       .withDayOfWeek(allWeekDays)

        _ <- ZIO.logInfo(s"The calendar: $myCalendar")

        schedule = ZSchedule
                     .withAction {
                       ZScheduleStartWorkflowStub.start(
                         stub.printGreeting("Hello!")
                       )
                     }
                     .withSpec(
                       ZScheduleSpec()
//                         .withStartAt(now.plusSeconds(60))
//                         .withIntervals(
//                           every(5.minutes)
//                         )
                         // todo: make calendars work
                         .withCalendars(myCalendar)
                     )

        handle <- scheduleClient.createSchedule(
                    scheduleId.toString,
                    schedule
                  )
//
        description <- handle.describe
        _           <- ZIO.logInfo(s"Created schedule=$description")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkflowServiceStubs.setup()
      _ <- startWorkflows
      // todo: not sure about the order
//      _ <- ZWorkerFactory.serve
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
