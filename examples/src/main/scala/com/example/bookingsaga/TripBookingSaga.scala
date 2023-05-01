package com.example.bookingsaga

import zio.*
import zio.temporal.*
import zio.temporal.worker.*
import zio.temporal.activity.*
import zio.temporal.workflow.*
import zio.logging.backend.SLF4J
import io.temporal.client.WorkflowException

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
             .catchAll { (e: WorkflowException) =>
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
