package com.example.bookingsaga

import zio.*
import zio.temporal.*
import zio.temporal.activity.*

object TripBookingActivitiesImpl {
  val make: URLayer[ZActivityOptions[Any], TripBookingActivitiesImpl] =
    ZLayer.fromFunction(new TripBookingActivitiesImpl()(_: ZActivityOptions[Any]))
}

class TripBookingActivitiesImpl(implicit options: ZActivityOptions[Any]) extends TripBookingActivities {
  override def reserveCar(name: String): String = {
    ZActivity.run {
      for {
        _     <- ZIO.logInfo(s"Reserve a car for '$name'")
        carId <- ZIO.randomWith(_.nextUUID)
      } yield carId.toString
    }
  }

  override def bookFlight(name: String): String = {
    ZActivity.run {
      for {
        _ <- ZIO.logError(s"Failing to book a flight for '$name'")
        _ <- ZIO.fail(new RuntimeException("Flight booking did not work"))
      } yield "dummy"
    }
  }

  override def bookHotel(name: String): String = {
    ZActivity.run {
      for {
        _      <- ZIO.logInfo(s"Booking a hotel room for '$name'")
        roomId <- ZIO.randomWith(_.nextUUID)
      } yield roomId.toString
    }
  }

  override def cancelFlight(reservationID: String, name: String): String = {
    ZActivity.run {
      for {
        _              <- ZIO.logInfo(s"Cancelling flight reservation '$reservationID' for '$name'")
        cancellationId <- ZIO.randomWith(_.nextUUID)
      } yield cancellationId.toString
    }
  }

  override def cancelHotel(reservationID: String, name: String): String = {
    ZActivity.run {
      for {
        _              <- ZIO.logInfo(s"Cancelling hotel reservation '$reservationID' for '$name'")
        cancellationId <- ZIO.randomWith(_.nextUUID)
      } yield cancellationId.toString
    }
  }

  override def cancelCar(reservationID: String, name: String): String = {
    ZActivity.run {
      for {
        _              <- ZIO.logInfo(s"Cancelling car reservation '$reservationID' for '$name'")
        cancellationId <- ZIO.randomWith(_.nextUUID)
      } yield cancellationId.toString
    }
  }
}
