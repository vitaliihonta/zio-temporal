package com.example.bookingsaga

import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.activity._

class TripBookingWorkflowImpl extends TripBookingWorkflow {
  private val activities: ZActivityStub.Of[TripBookingActivities] = ZWorkflow
    .newActivityStub[TripBookingActivities](
      ZActivityOptions
        .withStartToCloseTimeout(1.hour)
        .withRetryOptions(
          ZRetryOptions.default.withMaximumAttempts(1)
        )
    )

  override def bookTrip(name: String): Unit = {
    val bookingSaga = for {
      // Option 1: attempt and add compensation later
      carReservationID <- ZSaga.attempt(
                            ZActivityStub.execute(
                              activities.reserveCar(name)
                            )
                          )
      _ <- ZSaga.compensation(
             ZActivityStub.execute(
               activities.cancelCar(carReservationID, name)
             )
           )
      hotelReservationID <- ZSaga.attempt(
                              ZActivityStub.execute(
                                activities.bookHotel(name)
                              )
                            )
      // Option 2: make a ZSaga with main action and compensation
      flightReservationID <- ZSaga.make(
                               exec = ZActivityStub.execute(
                                 activities.bookFlight(name)
                               )
                             )(
                               compensate = ZActivityStub.execute(
                                 activities.cancelHotel(hotelReservationID, name)
                               )
                             )
      _ <- ZSaga.compensation(
             ZActivityStub.execute(
               activities.cancelFlight(flightReservationID, name)
             )
           )
    } yield ()

    bookingSaga.runOrThrow(
      options = ZSaga.Options(parallelCompensation = true)
    )
  }
}
