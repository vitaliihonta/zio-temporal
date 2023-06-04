package com.example.bookingsaga

import zio.temporal._

@workflowInterface
trait TripBookingWorkflow {
  @workflowMethod
  def bookTrip(name: String): Unit
}
