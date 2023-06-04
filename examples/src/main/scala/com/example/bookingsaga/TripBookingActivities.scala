package com.example.bookingsaga

import zio.temporal._

@activityInterface
trait TripBookingActivities {

  /** Request a car rental reservation.
    *
    * @param name
    *   customer name
    * @return
    *   reservationID
    */
  def reserveCar(name: String): String

  /** Request a flight reservation.
    *
    * @param name
    *   customer name
    * @return
    *   reservationID
    */
  def bookFlight(name: String): String

  /** Request a hotel reservation.
    *
    * @param name
    *   customer name
    * @return
    *   reservationID
    */
  def bookHotel(name: String): String

  /** Cancel a flight reservation.
    *
    * @param name
    *   customer name
    * @param reservationID
    *   id returned by bookFlight
    * @return
    *   cancellationConfirmationID
    */
  def cancelFlight(reservationID: String, name: String): String

  /** Cancel a hotel reservation.
    *
    * @param name
    *   customer name
    * @param reservationID
    *   id returned by bookHotel
    * @return
    *   cancellationConfirmationID
    */
  def cancelHotel(reservationID: String, name: String): String

  /** Cancel a car rental reservation.
    *
    * @param name
    *   customer name
    * @param reservationID
    *   id returned by reserveCar
    * @return
    *   cancellationConfirmationID
    */
  def cancelCar(reservationID: String, name: String): String
}
