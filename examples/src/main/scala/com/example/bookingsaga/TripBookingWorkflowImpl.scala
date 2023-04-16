package com.example.bookingsaga

import zio.*
import zio.temporal.*
import zio.temporal.workflow.*

class TripBookingWorkflowImpl extends TripBookingWorkflow {
  private val activities: TripBookingActivities = ZWorkflow
    .newActivityStub[TripBookingActivities]
    .withStartToCloseTimeout(1.hour)
    .withRetryOptions(
      ZRetryOptions.default.withMaximumAttempts(1)
    )
    .build

  override def bookTrip(name: String): Unit = {
    val bookingSaga = for {
      // Option 1: attempt and add compensation later
      carReservationID <- ZSaga.attempt(activities.reserveCar(name))
      _ <- ZSaga.compensation(
             activities.cancelCar(carReservationID, name)
           )
      hotelReservationID <- ZSaga.attempt(
                              activities.bookHotel(name)
                            )
      // Option 2: make a ZSaga with main action and compensation
      flightReservationID <- ZSaga.make(
                               activities.bookFlight(name)
                             )(activities.cancelHotel(hotelReservationID, name))
      _ <- ZSaga.compensation(
             activities.cancelFlight(flightReservationID, name)
           )
    } yield ()

    bookingSaga.runOrThrow(
      options = ZSaga.Options(parallelCompensation = true)
    )
  }
}
