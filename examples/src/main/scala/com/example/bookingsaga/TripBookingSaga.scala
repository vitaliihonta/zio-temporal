package com.example.bookingsaga

import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.activity._
import zio.temporal.workflow._
import zio.logging.backend.SLF4J
import io.temporal.failure.TemporalException

object TripBookingSaga extends ZIOAppDefault {
  val TaskQueue = "trip-booking"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addActivityImplementationService[TripBookingActivitiesImpl] @@
        ZWorker.addWorkflow[TripBookingWorkflow].from(new TripBookingWorkflowImpl)

    def bookingFlow(name: String) = for {
      client <- ZIO.service[ZWorkflowClient]
      tripId <- ZIO.randomWith(_.nextUUID)
      trip <- client
                .newWorkflowStub[TripBookingWorkflow]
                .withTaskQueue(TaskQueue)
                .withWorkflowId(tripId.toString)
                .build

      _ <- ZWorkflowStub
             .execute(
               trip.bookTrip(name)
             )
             .catchAll { (e: TemporalException) =>
               ZIO.logInfo(s"Workflow execution failed (as expected): $e")
             }
    } yield ()

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkerFactory.setup
      _ <- ZWorkflowServiceStubs.setup()
      // flights
      _ <- ZIO.collectAllParDiscard(
             List(
               bookingFlow("John Doe"),
               bookingFlow("Alice Johnson")
             )
           )
    } yield ()

    program.provideSome[Scope](
      ZWorkflowServiceStubsOptions.make,
      ZWorkflowClientOptions.make,
      ZWorkerFactoryOptions.make,
      ZActivityOptions.default,
      // Activity
      TripBookingActivitiesImpl.make,
      // Services
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make,
      ZWorkerFactory.make
    )
  }
}
