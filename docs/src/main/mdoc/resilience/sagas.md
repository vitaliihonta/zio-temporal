# Sagas

<head>
  <meta charset="UTF-8" />
  <meta name="description" content="ZIO Temporal Saga Pattern" />
  <meta name="keywords" content="ZIO Temporal saga pattern, Scala Temporal saga pattern" />
</head>

Managing distributed transactions can be difficult to do well. Sagas are one of the most tried and tested design patterns for long-running work:
- A Saga provides transaction management using a sequence of local transactions. 
- A local transaction is the unit of work performed by a saga participant, a microservice. 
- Every operation that is part of the Saga can be rolled back by a compensating transaction. 
- The Saga pattern guarantees that either all operations are completed successfully or the corresponding compensation transactions are run to undo the previously completed work.

Implementing the Saga pattern can be complex, but fortunately, Temporal provides native support for the Saga pattern. It means that handling all the rollbacks and running compensation transactions are performed internally by Temporal.

Let's start with some basic imports that will be required for the whole demonstration:

## Example
Imagine that we provide a service where people can book a trip. Booking a regular trip often consists of several steps:
- Booking a car.
- Booking a hotel.
- Booking a flight.

The customer either wants everything to be booked or nothing at all. There is no sense in booking a hotel without booking a plane. Also, imagine that each booking step in this transaction is represented via a dedicated service or microservice.

All of these steps together make up a **distributed transaction** that crosses multiple services and databases. To ensure a successful booking, all three microservices must complete the individual local transactions.  
If any of the steps fail, all the completed preceding transactions should be reversed accordingly. We cannot simply "delete" the prior transactions or "go back in time" - particularly where money and bookings are concerned, it is important to have an immutable record of attempts and failures.  
Therefore, we should accumulate a list of compensating actions to execute when failure occurs.

You can find the full example [here](https://github.com/vitaliihonta/zio-temporal/tree/main/examples/src/main/scala/com/example/bookingsaga).

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.workflow._
import zio.temporal.activity._
```

## Workflow interface
The first thing we need to do is to write a business process - the high-level flow of the trip booking. Let's call it TripBookingWorkflow:

```scala mdoc:silent
@workflowInterface
trait TripBookingWorkflow {
  @workflowMethod
  def bookTrip(name: String): Unit
}
```

For simplicity, let's assume that all booking services (car, hotel, and flight) are managed under one single activity interface `TripBookingActivities`.  
But it is not a requirement.

```scala mdoc:silent
@activityInterface
trait TripBookingActivities {

  /** Request a car rental reservation.
    */
  def reserveCar(name: String): String

  /** Request a flight reservation.
    */
  def bookFlight(name: String): String

  /** Request a hotel reservation.
    */
  def bookHotel(name: String): String

  /** Cancel a flight reservation.
    */
  def cancelFlight(reservationID: String, name: String): String

  /** Cancel a hotel reservation.
    */
  def cancelHotel(reservationID: String, name: String): String

  /** Cancel a car rental reservation.
    */
  def cancelCar(reservationID: String, name: String): String
}
```

## Write the Saga

`ZSaga` data type allows creating Sagas in Temporal. Let's see how to use it in practice:  

```scala mdoc:silent
class TripBookingWorkflowImpl extends TripBookingWorkflow {
  // Create the activity stub 
  private val activities = ZWorkflow
    .newActivityStub[TripBookingActivities]
    .withStartToCloseTimeout(1.hour)
    .withRetryOptions(
      ZRetryOptions.default.withMaximumAttempts(1)
    )
    .build

  override def bookTrip(name: String): Unit = {
    val bookingSaga: ZSaga[Unit] = for {
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
```

**Notes**:
**(1)** There is multiple ways to create a `ZSaga`:
- `ZSaga.attempt` wraps code that may fail
- `ZSaga.compensation` adds compensation action
- `ZSaga.make` is basically `attempt` followed by a `compensation`

**(2)** It's also possible to create `ZSaga`s from values:
- `ZSaga.succeed` wraps an existing value
- `ZSaga.fail` wraps an error (that will be compensated)
- `ZSaga.fromEither` wraps an `Either` data type and compensates in case it's `Left`
- `ZSaga.fromTry` wraps a `scala.util.Try` data type and compensates in case it's a `Failure`

**(3)** There are a few ways to combine multiple `ZSaga`s:
- `for` comprehension (`map`, `flatMap`)
- `unit` - ignores the result value
- `catchAll`/`catchSome` - handle errors
- `ZSaga.foreach` - iterates over a collection & chains Sagas
- ... and more, much like `ZIO` data type has

**(4)** To run the saga, use:
- `run` method (returns `Either` with the error or the result) 
- `runOrThrow` (throws an exception in case of failure)
- Both method run compensations in case of a failure.
- `ZSaga.Options` allows specifying Saga's behavior:
  - `parallelCompensation` - this decides if the compensation operations are run in parallel. It's `false` by default
  - `continueWithError` - whether to proceed with the next compensation operation if the previous throws exception. It's `false` by default
