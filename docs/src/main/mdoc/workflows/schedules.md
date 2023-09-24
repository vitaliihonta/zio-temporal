# Schedules

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal schedules" />
  <meta name="keywords" content="ZIO Temporal schedules, Scala Temporal schedules" />
</head>


Scheduling Workflows is a crucial aspect of any automation process, especially when dealing with time-sensitive tasks.  
By scheduling a Workflow, you can automate repetitive tasks, reduce the need for manual intervention, and ensure timely
execution of your business processes.

Use any of the following action to help Schedule a Workflow Execution and take control over your automation process.

## How to Create a Scheduled Workflow

The create action enables you to create a new Schedule.  
When you create a new Schedule, a unique Schedule ID is generated, which you can use to reference the Schedule in other
Schedule commands.

Start with the following workflow

```scala mdoc:silent
import zio.temporal._
import zio.temporal.workflow._

@workflowInterface
trait HelloWorkflowWithTime {

  @workflowMethod
  def printGreeting(name: String): Unit
}

class HelloWorkflowWithTimeImpl extends HelloWorkflowWithTime {
  private val logger = ZWorkflow.makeLogger

  override def printGreeting(name: String): Unit = {
    logger.info(s"Hello $name, it's ${ZWorkflow.currentTimeMillis.toLocalDateTime()} now")
  }
}

```

A Temporal worker is created as usual.

To create a Scheduled Workflow Execution, use `ZScheduleClient.createSchedule` method.  
Then pass the Schedule ID and the Schedule object to the method to create a Scheduled Workflow Execution.  
Set the action parameter to ScheduleActionStartWorkflow to start a Workflow Execution.  
Optionally, you can set the spec parameter to ScheduleSpec to specify the schedule or set the intervals parameter to
ScheduleIntervalSpec to specify the interval.  
Other options include: `cronExpressions`, `skip`, `startAt`, `endAt`, and `jitter`.

```scala mdoc:silent
import zio._
import zio.temporal.schedules._
import zio.temporal.worker._

val scheduleHandleZIO: RIO[ZScheduleClient, ZScheduleHandle] = ZIO.serviceWithZIO[ZScheduleClient] { scheduleClient =>
  for {
    now <- Clock.instant

    // Create a ZScheduleStartWorkflowStub
    stub = scheduleClient
      .newScheduleStartWorkflowStub[HelloWorkflowWithTime](
        ZWorkflowOptions
          .withWorkflowId("<workflow-id>")
          .withTaskQueue("<task-queue>")
      )

    // Schedule specification
    intervalSpec = ZScheduleSpec
      .intervals(every(15.minutes))
      .withSkip(
        // skip weekends
        calendar
          .withDayOfWeek(weekend)
          .withComment("Except weekend")
      )

    // Create schedule
    schedule = ZSchedule
      .withAction {
        ZScheduleStartWorkflowStub.start(
          stub.printGreeting("Hello!")
        )
      }
      .withSpec(intervalSpec)

    // returns a schedule handle 
    // that can be used to interact with the schedule
    handle <- scheduleClient.createSchedule(
      "<schedule-id>",
      schedule
    )

    // Describing the schedule
    description <- handle.describe
    _ <- ZIO.logInfo(s"Created schedule=$description")
  } yield handle
}
```

Important notes:

- You need a `ZScheduleClient` instance to create a schedule. You should either access it via ZIO environment or have it
  already created somewhere
- **You must always** wrap the workflow method invocation into `ZScheduleStartWorkflowStub.start` method.
    - The `ZScheduleStartWorkflowStub.Of[HelloWorkflowWithTime]` is a compile-time stub, so actual method invocations
      are only valid in compile-time
    - `stub.printGreeting("Hello!")` invocation would be re-written into an untyped Temporal's workflow schedule action
      specification
    - A direct method invocation will throw an exception
- The `ZScheduleStartWorkflowStub` is basically a proxy, which creates the schedule

## How to Backfill a Scheduled Workflow

The backfill action executes Actions ahead of their specified time range. This command is useful when you need to
execute a missed or delayed Action, or when you want to test the Workflow before its scheduled time.

To Backfill a Scheduled Workflow Execution, use the `backfill` method on the `ZScheduleHandle`.

```scala mdoc:silent
for {
  // Get the handle
  handle <- scheduleHandleZIO
  now <- Clock.instant

  _ <- handle.backfill(
    List(
      ZScheduleBackfill(
        startAt = now.minusMillis(1.day.toMillis),
        endAt = now.minusMillis(1.hour.toMillis)
      )
    )
  )
} yield ()
```

## How to Delete a Scheduled Workflow

The delete action enables you to delete a Schedule. When you delete a `ZSchedule`, it does not affect any Workflows that
were started by the `ZSchedule`.

To delete a Scheduled Workflow Execution, use the `delete` method on the `ZScheduleHandle`.

```scala mdoc:silent
for {
  // Get the handle
  handle <- scheduleHandleZIO
  _ <- handle.delete()
} yield ()
```

## How to List a Scheduled Workflow

The list action lists all the available Schedules. This command is useful when you want to view a list of all the
Schedules and their respective Schedule IDs.

To list all schedules, use the `listSchedules` method on the `ZScheduleClient`. It returns a `ZStream` of available schedules. If a schedule is added or deleted, it
may not be available in the list immediately.

```scala mdoc:silent
for {
  scheduleClient <- ZIO.service[ZScheduleClient]
  _ <- scheduleClient
    .listSchedules()
    .mapZIO(schd => ZIO.logInfo(s"Found schedule=$schd"))
    .runDrain
} yield ()
```

## How to Pause a Scheduled Workflow
The pause action enables you to pause and unpause a Schedule. When you pause a Schedule, all the future Workflow Runs associated with the Schedule are temporarily stopped. This command is useful when you want to temporarily halt a Workflow due to maintenance or any other reason.

To pause a Scheduled Workflow Execution, use the `pause` method on the `ZScheduleHandle`. You can pass a note to the `pause` method to provide a reason for pausing the schedule.

```scala mdoc:silent
for {
  // Get the handle
  handle <- scheduleHandleZIO
  _ <- handle.pause(note = Some("Pausing the schedule for now"))
} yield ()
```

## How to Trigger a Scheduled Workflow
The trigger action triggers an immediate action with a given Schedule. By default, this action is subject to the Overlap Policy of the Schedule. This command is helpful when you want to execute a Workflow outside of its scheduled time.

To trigger a Scheduled Workflow Execution, use the `trigger` method on the `ZScheduleHandle`.

```scala mdoc:silent
for {
  // Get the handle
  handle <- scheduleHandleZIO
  _ <- handle.trigger()
} yield ()
```

## How to Update a Scheduled Workflow
The update action enables you to update an existing `ZSchedule`. This command is useful when you need to modify the Schedule's configuration, such as changing the start time, end time, or interval.

Create a function that takes `ZScheduleUpdateInput` and returns `ZScheduleUpdate`. To update a `ZSchedule`, use a callback to build the update from the description. The following example updates the `ZSchedule` to use a new schedule spec and a new workflow argument.

```scala mdoc:silent
for {
  scheduleClient <- ZIO.service[ZScheduleClient]
  now <- Clock.instant
  // Get the handle
  handle <- scheduleHandleZIO
  // update logic
  _ <- handle.update { input =>
    // new schedule spec
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

    // need a new stub to update the workflow
    val newStub = scheduleClient.newScheduleStartWorkflowStub[HelloWorkflowWithTime](
      ZWorkflowOptions
        .withWorkflowId("<workflow-id>")
        .withTaskQueue("<task-queue>")
    )

    ZScheduleUpdate(
      input.description.schedule
        // change workflow arguments
        .withAction(
          ZScheduleStartWorkflowStub.start(
            newStub.printGreeting("Hello updated!")
          )
        )
        .withSpec(calendarSpec)
    )
  }
} yield ()
```
