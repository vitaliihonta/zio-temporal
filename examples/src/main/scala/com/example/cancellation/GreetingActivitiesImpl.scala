package com.example.cancellation

import io.temporal.client.ActivityCompletionException
import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*

object GreetingActivitiesImpl {
  val make: URLayer[ZActivityOptions[Any], GreetingActivities] =
    ZLayer.fromFunction(new GreetingActivitiesImpl()(_: ZActivityOptions[Any]))
}

class GreetingActivitiesImpl(implicit options: ZActivityOptions[Any]) extends GreetingActivities {
  override def composeGreeting(greeting: String, name: String): String = {
    val context = ZActivity.executionContext

    ZActivity.run {
      def loop(i: Int, seconds: Int): Task[Unit] =
        if (i >= seconds) ZIO.unit
        else
          for {
            _ <- ZIO.sleep(1.second)
            // Perform the heartbeat. Used to notify the workflow that activity execution is alive
            _ <- context.heartbeat(i).onError { _ =>
                   /*
                    * Activity heartbeat can throw an exception for multiple reasons, including:
                    * 1) activity cancellation
                    * 2) activity not existing (due to a timeout for example) from the service point of view
                    * 3) activity worker shutdown request
                    *
                    * In our case our activity fails because one of the other performed activities
                    * has completed execution and our workflow method has issued the "cancel" request
                    * to cancel all other activities in the cancellation scope.
                    *
                    * The following code simulates our activity after cancellation "cleanup"
                    */
                   cleanup()
                 }
            _ <- loop(i + 1, seconds)
          } yield ()

      def cleanup(): UIO[Unit] = {
        for {
          seconds <- ZIO.randomWith(_.nextIntBetween(0, GreetingWorkflowImpl.ActivityMaxCleanupSeconds))
          _       <- ZIO.logInfo(s"Activity for $greeting was cancelled. Cleanup is expected to take $seconds seconds")
          _       <- ZIO.sleep(seconds.seconds)
          _       <- ZIO.logInfo(s"Activity for $greeting finished cancellation")
        } yield ()
      }

      for {
        // simulate a random time this activity should execute for
        seconds <- ZIO.randomWith(_.nextIntBetween(0, GreetingWorkflowImpl.ActivityMaxSleepSeconds).map(_ + 5))
        _       <- ZIO.logInfo(s"Activity for $greeting is going to take $seconds seconds")
        _       <- loop(0, seconds)
        _       <- ZIO.logInfo(s"Activity for $greeting completed")
      } yield s"$greeting $name!"
    }
  }
}
