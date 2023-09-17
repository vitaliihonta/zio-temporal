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

    def startSchedules(workflowId: String, scheduleId: String) = ZIO.serviceWithZIO[ZScheduleClient] { scheduleClient =>
      val stub = scheduleClient.newScheduleStartWorkflowStub[HelloWorkflowWithTime](
        ZWorkflowOptions
          .withWorkflowId(workflowId)
          .withTaskQueue(TaskQueue)
      )

      val intervalSpec = ZScheduleSpec
        .intervals(every(15.minutes))
        .withSkip(
          // skip weekends
          calendar
            .withDayOfWeek(weekend)
            .withComment("Except weekend")
        )

      val schedule = ZSchedule
        .withAction {
          ZScheduleStartWorkflowStub.start(
            stub.printGreeting("Hello!")
          )
        }
        .withSpec(intervalSpec)

      for {
        handle <- scheduleClient.createSchedule(
                    scheduleId.toString,
                    schedule
                  )

        description <- handle.describe
        _           <- ZIO.logInfo(s"Created schedule=$description")
        _ <- scheduleClient
               .listSchedules()
               .mapZIO(schd => ZIO.logInfo(s"Found schedule=$schd"))
               .runDrain
      } yield handle
    }

    def manipulateWithSchedule(
      handle:     ZScheduleHandle,
      workflowId: String
    ): RIO[ZScheduleClient, Unit] = {
      ZIO.serviceWithZIO[ZScheduleClient] { scheduleClient =>
        for {
          _   <- ZIO.logInfo(s"Manually triggering schedule=${handle.id}")
          _   <- handle.trigger()
          _   <- ZIO.sleep(5.seconds)
          _   <- ZIO.logInfo(s"Pausing schedule=${handle.id}")
          _   <- handle.pause(note = Some("Temporarily pause"))
          _   <- ZIO.sleep(30.seconds)
          _   <- ZIO.logInfo(s"Unpausing schedule=${handle.id}")
          _   <- handle.unpause(note = Some("Unpause"))
          _   <- ZIO.logInfo(s"Backfill schdule=${handle.id}")
          now <- Clock.instant
          _ <-
            handle.backfill(
              List(
                ZScheduleBackfill(startAt = now.minusMillis(1.day.toMillis), endAt = now.minusMillis(1.hour.toMillis))
              )
            )
          _ <- ZIO.sleep(30.seconds)
          _ <- ZIO.logInfo(s"Updating schedule=${handle.id}")
          _ <- handle.update { input =>
                 val calendarSpec = ZScheduleSpec
                   .calendars(
                     calendar
                       .withSeconds(range())
                       .withMinutes(range(to = 59, by = 4))
                       .withHour(range(from = 1, to = 23, by = 2))
                       .withDayOfMonth(allMonthDays)
                       .withMonth(allMonths)
                       .withDayOfWeek(allWeekDays)
                       .withComment("Every odd hour, every 10 minutes during an hour")
                   )
                   .withStartAt(now.plusSeconds(10))

                 val newStub = scheduleClient.newScheduleStartWorkflowStub[HelloWorkflowWithTime](
                   ZWorkflowOptions
                     .withWorkflowId(workflowId)
                     .withTaskQueue(TaskQueue)
                 )

                 ZScheduleUpdate(
                   input.description.schedule
                     // change workflow parameters
                     .withAction(
                       ZScheduleStartWorkflowStub.start(
                         newStub.printGreeting("Hello updated!")
                       )
                     )
                     .withSpec(calendarSpec)
                 )
               }
          _ <- ZIO.sleep(30.seconds)
          _ <- ZIO.logInfo("Triggering to demonstrate updated arguments...")
          _ <- handle.trigger()
          _ <- ZIO.sleep(30.seconds)
          _ <- ZIO.logInfo(s"Deleting schedule=${handle.id}")
          _ <- handle.delete()
        } yield ()
      }
    }

    val program = for {
      _          <- registerWorkflows
      _          <- ZWorkflowServiceStubs.setup()
      scheduleId <- Random.nextUUID
      workflowId <- Random.nextUUID
      handle     <- startSchedules(workflowId.toString, scheduleId.toString)
      _          <- ZWorkerFactory.setup
      _          <- manipulateWithSchedule(handle, workflowId.toString)
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
