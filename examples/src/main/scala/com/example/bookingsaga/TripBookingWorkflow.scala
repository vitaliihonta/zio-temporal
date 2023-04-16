package com.example.bookingsaga

import zio.temporal.*

@workflowInterface
trait TripBookingWorkflow {
  @workflowMethod
  def bookTrip(name: String): Unit
}
