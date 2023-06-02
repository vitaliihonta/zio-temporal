package com.example.cancellation

import io.temporal.activity.ActivityCancellationType
import io.temporal.client.ActivityCompletionException
import io.temporal.failure.{CanceledFailure, TimeoutFailure}
import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.failure.ActivityFailure
import zio.temporal.workflow.*

object GreetingWorkflowImpl {
  val ActivityMaxSleepSeconds: Int            = 30
  val ActivityMaxCleanupSeconds: Int          = 5
  val ActivityStartToCloseTimeoutSeconds: Int = ActivityMaxSleepSeconds + ActivityMaxCleanupSeconds
}

class GreetingWorkflowImpl extends GreetingWorkflow {
  private val greetings = List("Hello", "Hola", "Привіт", "Ahoi", "Hallo")

  /** Define the GreetingActivities stub. Activity stubs are proxies for activity invocations that are executed outside
    * of the workflow thread on the activity worker, that can be on a different host. Temporal is going to dispatch the
    * activity results back to the workflow and unblock the stub as soon as activity is completed on the activity
    * worker.
    *
    * <p>The "startToCloseTimeout" option sets the maximum time of a single Activity execution attempt. For this example
    * it is set to 10 seconds.
    *
    * <p>The "cancellationType" option means that in case of activity cancellation the activity should fail with
    * [[io.temporal.failure.CanceledFailure]]. We set ActivityCancellationType.WAIT_CANCELLATION_COMPLETED which denotes
    * that activity should be first notified of the cancellation, and cancelled after it can perform some cleanup tasks
    * for example. Note that an activity must heartbeat to receive cancellation notifications.
    */
  private val activities: ZActivityStub.Of[GreetingActivities] = ZWorkflow
    .newActivityStub[GreetingActivities]
    .withStartToCloseTimeout(GreetingWorkflowImpl.ActivityStartToCloseTimeoutSeconds.seconds)
    .withHeartbeatTimeout(5.seconds)
    .withCancellationType(ActivityCancellationType.WAIT_CANCELLATION_COMPLETED)
    .build

  override def getGreeting(name: String): String = {
    var activityResult: List[ZAsync[String]] = Nil

    /*
     * Create our CancellationScope. Within this scope we call the workflow activity
     * composeGreeting method asynchronously for each of our defined greetings in different
     * languages.
     */
    val scope = ZWorkflow.newCancellationScope {
      activityResult = greetings.map { greeting =>
        ZActivityStub.executeAsync(
          activities.composeGreeting(greeting, name)
        )
      }
    }

    /*
     * Execute all activities within the CancellationScope. Note that this execution is
     * non-blocking as the code inside our cancellation scope is also non-blocking.
     */
    scope.run()

    // We use "raceFirst" here to wait for one of the activity invocations to return
    val result = ZAsync.raceFirst(activityResult).run.getOrThrow

    // Trigger cancellation of all uncompleted activity invocations within the cancellation scope
    scope.cancel("Just for fun")

    /*
     *  Wait for all activities to perform cleanup if needed.
     *  For the sake of the example we ignore cancellations and
     *  get all the results so that we can print them in the end.
     *
     *  Note that we cannot use "allOf" here as that fails on any Promise failures
     */
    for (result <- activityResult) {
      try {
        result.run.getOrThrow
      } catch {
        case ActivityFailure.Cause(_: CanceledFailure | _: TimeoutFailure) =>
        // safe to ignore
      }
    }

    result
  }
}
