[![zio-temporal-core Scala version support](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core/latest-by-scala-version.svg?platform=jvm)](https://index.scala-lang.org/vitaliihonta/zio-temporal/zio-temporal-core)
![Build status](https://github.com/vitaliihonta/zio-temporal/actions/workflows/publish.yaml/badge.svg)
[![codecov](https://codecov.io/gh/vitaliihonta/zio-temporal/branch/main/graph/badge.svg?token=T8NBC4R360)](https://codecov.io/gh/vitaliihonta/zio-temporal)

# ZIO Temporal

This is an integration with [Temporal workflow](https://temporal.io) based on Java SDK and ZIO.  
It allows you to define and use workflows in a Scala way!

[TL;DR intro about Temporal workflow](https://youtu.be/2HjnQlnA5eY)

## Modules

1. **zio-temporal-core** - ZIO integration and basic wrappers that bring type safety to your workflows.  
   Allows you to use arbitrary ZIO code in your activities
2. **zio-temporal-scalapb** - integration with [ScalaPB](https://scalapb.github.io/) which allows you to
   use [Protobuf](https://developers.google.com/protocol-buffers)  
   as a transport layer protocol for communication with Temporal cluster
3. **zio-temporal-distage** - provides a concise integration with [DIstage](https://izumi.7mind.io/distage/index.html)
   which allows  
   to use Dependency Injection for building Workflows and Activities

## Installation

```sbt
// Core
libraryDependencies += "dev.vhonta" %% "zio-temporal-core" % "<VERSION>"

// ScalaPB integration
libraryDependencies += "dev.vhonta" %% "zio-temporal-scalapb" % "<VERSION>"

// DIstage integration**** integration
libraryDependencies += "dev.vhonta" %% "zio-temporal-distage" % "<VERSION>"
```

## Examples

You can find the source code of example workflow in [examples directory](./examples)

### Defining workflows

When you work with **ZIO Temporal**, first you need to define [Activities](https://docs.temporal.io/activities)
and [Workflows](https://docs.temporal.io/workflows).

Lets started with protocol buffers:

```protobuf
syntax = "proto2";

package com.example.transactions;

// ZIO Temporal provides some useful types for your protocols, like UUID, BigDecimal, etc.
import "zio.temporal.proto";

// Workflow method input - request to proceed a transaction
message ProceedTransactionCommand {
  required zio.temporal.proto.UUID id = 1;
  required zio.temporal.proto.UUID sender = 2;
  required zio.temporal.proto.UUID receiver = 3;
  required zio.temporal.proto.BigDecimal amount = 4;
}

// User confirms the transaction with CVV code
message ConfirmTransactionCommand {
  required zio.temporal.proto.UUID id = 1;
  required string confirmationCode = 2;
}

// Cancel transaction execution
message CancelTransactionCommand {
  required zio.temporal.proto.UUID id = 1;
}

// Statuses
enum TransactionStatus {
  Created = 0;
  InProgress = 1;
  Succeeded = 2;
  Failed = 3;
}

// Snapshot of the transaction state
message TransactionView {
  required zio.temporal.proto.UUID id = 1;
  required TransactionStatus status = 2;
  required string description = 3;
  required zio.temporal.proto.UUID sender = 4;
  required zio.temporal.proto.UUID receiver = 5;
  required zio.temporal.proto.BigDecimal amount = 6;
}

// Possible error
message TransactionError {
  required int32 code = 1;
  required string message = 2;
}
```

Then you could define your workflow and activity using them

```scala
import com.example.transactions._
import zio.temporal._
import zio.temporal.proto.ZUnit

// Notice the annotation! It's required by Java SDK
@activity
trait PaymentActivity {

  // Initiate the transaction (for instance by interacting with Banking system)
  def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView]

  // Confirm the transaction with user's CVV code
  def verifyConfirmation(command: ConfirmTransactionCommand): Either[TransactionError, ZUnit]

  // Cancel transaction execution
  def cancelTransaction(command: CancelTransactionCommand): Either[TransactionError, ZUnit]
}

// Notice the annotation! It's required by Java SDK
@workflow
trait PaymentWorkflow {

  // Method to start the workflow (possibly long running)
  @workflowMethod
  def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView]

  // Allows to query workflow state
  @queryMethod
  def getStatus: Either[TransactionError, TransactionView]

  // Allows to interact with workflow from the outside
  // In this case, it's transaction confirmation with customer provided CVV code 
  @signalMethod
  def confirmTransaction(command: ConfirmTransactionCommand): Unit
}
```

### Implementing workflows

Activity is supposed to run arbitrary side effects, while Workflow is deterministic and is built of activities.

That means that you'll use ZIO only inside activities:

```scala
// Library imports
import zio._
import zio.temporal.activity.ZActivity
import zio.temporal.activity.ZActivityOptions
import zio.temporal.proto.ZUnit

// The protocol and activities
import com.example.transactions._
import com.example.payments.workflows.PaymentActivity

class PaymentActivityImpl(/*Any dependencies you require*/)(
  implicit options: ZActivityOptions // required to run activities
) extends PaymentActivity /*Extends activity interface*/ {

  override def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView] =
    // Asynchronously runs your ZIO so that Temporal may catch-up its result
    ZActivity.run {
      proceedImpl(transaction)
    }

  override def verifyConfirmation(command: ConfirmTransactionCommand): Either[TransactionError, ZUnit] =
    ZActivity.run {
      verifyConfirmationImpl(command)
    }

  override def cancelTransaction(command: CancelTransactionCommand): Either[TransactionError, ZUnit] =
    ZActivity.run {
      cancelTransactionImpl(command)
    }

  // Arbitrary zio code
  private def proceedImpl(command: ProceedTransactionCommand): ZIO[ZEnv, TransactionError, TransactionView] = ???

  // Arbitrary zio code
  private def verifyConfirmationImpl(command: ConfirmTransactionCommand): ZIO[ZEnv, TransactionError, Unit] = ???

  // Arbitrary zio code
  private def cancelTransactionImpl(command: CancelTransactionCommand): ZIO[ZEnv, TransactionError, Unit] = ???
}
```

Then you could use the Activity inside a Workflow:

```scala
// Library imports
import zio._
import zio.temporal._
import zio.temporal.saga._
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow.ZWorkflow

// Protocol imports
import com.example.transactions._
import com.example.payments.workflows._

import scala.concurrent.duration._

// Represents  the Workflow state
case class TransactionState(transaction: TransactionView, confirmation: Option[ConfirmTransactionCommand])

class PaymentWorkflowImpl(/*Any dependencies you require*/) extends PaymentWorkflow {

  // You may use any logging library you'd like to.
  private val logger = ???

  // This is how stubs are built.
  // They allow to invoke Activities via Temporal cluster 
  private val activity = ZWorkflow
    // Specify which Activity interface to use
    .newActivityStub[PaymentActivity]
    // Generic timeout for the Activity invocation
    .withStartToCloseTimeout(5.seconds)
    // Retry policy for Activity invocations
    .withRetryOptions(ZRetryOptions.default.withMaximumAttempts(1))
    .build

  // ZWorkflowState is a safe wrapper around mutable vars.
  // It's safe to use them here, because Workflow is kinda similar to persistent actor
  private val state = ZWorkflowState.empty[TransactionState]

  override def proceed(transaction: ProceedTransactionCommand): Either[TransactionError, TransactionView] = {
    // Provide initial state for the Workflow based on the incoming transaction
    state.setTo(initialState(transaction))

    // The code below is a distributed saga (https://microservices.io/patterns/data/saga.html) 
    // Combining sagas in `for comprehension`, 
    // you automatically accumulate compensations to revert the saga  
    val saga = for {
      created <- proceedTransaction(transaction)
      _ = logger.info(s"Initiated transaction $created")
      _ <- updateStateWith(created)
      _ = logger.info("Waiting for confirmation")
      _ <- waitForConfirmation()
      _ = logger.info("Handling confirmation")
      _ <- handleConfirmation()
      _ = logger.info("Transaction processed successfully")
    } yield state.snapshot.transaction

    // Run the saga. If an error occur, it automatically runs compensations.
    val result = saga.run()

    // You may handle errors here
    result.left.foreach { error =>
      failTransaction(error.message)
      logger.error(s"Transaction failed code=${error.code} message=${error.message}")
    }
    result
  }

  // Example of querying workflow state
  override def getStatus: Either[TransactionError, TransactionView] =
    state
      .toEither(TransactionError(code = 1, message = "Transaction not initialized"))
      .map(_.transaction)

  // Example of external signals which update Workflow state
  override def confirmTransaction(command: ConfirmTransactionCommand): Unit =
    state.updateWhen {
      case state if state.transaction.status.isInProgress =>
        state.copy(confirmation = Some(command))
    }

  private def initialState(command: ProceedTransactionCommand): TransactionState =
    ???

  // Definition of ZSaga with compensation
  private def proceedTransaction(command: ProceedTransactionCommand): ZSaga[TransactionError, TransactionView] =
    ZSaga.make(activity.proceed(command))(
      compensate = cancelTransaction()
    )

  // Possible compensation
  private def cancelTransaction(): Unit = ???


  // Allows to suspend the Workflow upon receiving external signals.
  // This will kill the thread which runs this Workflow 
  // so that Temporal cluster doesn't waste resources
  private def waitForConfirmation(): ZSaga[Nothing, Unit] =
    ZSaga.succeed {
       // Suspend condition based on Workflow state
       ZWorkflow.awaitWhile(
          state.snapshotOf(_.transaction.status) == TransactionStatus.InProgress &&
            state.snapshotOf(_.confirmation).isEmpty
       )
    }
  
  private def handleConfirmation(): ZSaga[TransactionError, Unit] = {
    val currentState = state.snapshot
    if (currentState.transaction.status.isFailed) {
      // You may fail the ZSaga with an error which will trigger compensations
      ZSaga.fail(TransactionError(code = -1, message = "Transaction failed"))
    } else
      currentState.confirmation match {
        case None => ZSaga.fail(TransactionError(code = 500, message = "Invalid transaction state"))
        // Do some work when transaction is completed
        case Some(confirmation) => verifyConfirmation(confirmation)
      }
  }

  // Invoke another Activity method from inside a ZSaga 
  private def verifyConfirmation(confirmation: ConfirmTransactionCommand): ZSaga[TransactionError, Unit] =
    ZSaga
      .make(activity.verifyConfirmation(confirmation))(compensate = cancelTransaction())
      .map(_ => finalizeTransaction())
      .unit

  private def failTransaction(description: String): ZSaga[Nothing, Unit] = ???

  private def finalizeTransaction(): Unit = ???

  private def updateStateWith(transaction: TransactionView): ZSaga[Nothing, Unit] = ???
}
```

### Using workflows from the outside

This is an example how you could trigger Workflow execution and interact with it  
by sending Signals and Querying the state:

```scala
import com.example.payments.workflows.PaymentWorkflow
import com.example.transactions._
import logstage.LogIO
import zio.temporal.workflow._
import zio.temporal.signal._
import zio.temporal.proto.syntax._
import zio._
import zio.duration._
import java.util.UUID

class ExampleFlow(client: ZWorkflowClient, rootLogger: LogIO[UIO]) {

  def proceedPayment(): URIO[ZEnv, Unit] =
    random.nextUUID.flatMap { transactionId =>
      val logger = rootLogger("transaction_id" -> transactionId)
      val paymentFlow = for {
        sender   <- random.nextUUID
        receiver <- random.nextUUID
        // This is how you build Workflow Stubs to start the workflow
        paymentWorkflow <- client
                             .newWorkflowStub[PaymentWorkflow]
                             .withTaskQueue("payments")
                             .withWorkflowId(transactionId.toString)
                             .build
        _            <- logger.info("Going to trigger workflow")
        // This starts the workflow
        _            <- initiateTransaction(paymentWorkflow)(transactionId, sender, receiver)
        _            <- logger.info("Trxn workflow started id")
        // Simulates user activity
        _            <- simulateUserActivity(logger)
        // This is how you create a stub to send signals and to query state
        workflowStub <- client.newUntypedWorkflowStub(workflowId = transactionId.toString)
        // Querying state
        currentState <- checkStatus(workflowStub, logger)(transactionId)
        _            <- logger.info(s"Trxn status checked $currentState")
        _            <- simulateUserActivity(logger)
        _            <- logger.info("Going to send confirmation")
        // Sending signals
        _            <- sendConfirmation(workflowStub, paymentWorkflow)(transactionId)
        _            <- logger.info("Confirmation sent!")
        _            <- simulateUserActivity(logger)
        // Status polling
        _ <- (clock.sleep(100.millis) *> checkStatus(workflowStub, logger)(transactionId))
                .repeatWhile(isNotFinished)
        _ <- logger.info("End-up polling status, fetching the result")
        // Retrieve the workflow result when it completes
        result <- workflowStub.resultEither[TransactionError, TransactionView]
        _      <- logger.info(s"Transaction finished $result")
      } yield ()

      paymentFlow.catchAll { error =>
        logger.error(s"Error processing transaction: $error")
      }
    }

  private def initiateTransaction(
    paymentWorkflow: ZWorkflowStub.Of[PaymentWorkflow]
  )(transactionId:   UUID,
    sender:          UUID,
    receiver:        UUID
  ) = {
    // Statically-typed workflow invocation.
    // ZIO Temporal automatically picks up the method name via macros.
    (paymentWorkflow.proceed _).start(
      // Provide input arguments
      ProceedTransactionCommand(
        id = transactionId.toProto,
        sender = sender.toProto,
        receiver = receiver.toProto,
        amount = BigDecimal(9000).toProto
      )
    )
  }

  // Query state
  private def checkStatus(workflowStub: ZWorkflowStub, logger: LogIO[UIO])(transactionId: UUID) = {
    logger.info("Checking transaction status...") *>
      workflowStub
        // Statically-typed query method invocation.
        // ZIO Temporal automatically picks up the method name via macros.
        .query0((_: PaymentWorkflow).getStatus)
        .runEither
  }

   private def sendConfirmation(
    workflowStub:    ZWorkflowStub,
    paymentWorkflow: ZWorkflowStub.Of[PaymentWorkflow]
  )(transactionId:   UUID
  ) = {
   // Statically-typed signal method invocation.
   // ZIO Temporal automatically picks up the method name via macros.
    workflowStub.signal(
      ZSignal.signal(paymentWorkflow.confirmTransaction _)
    )(
      zinput(
        // Signal method input
        ConfirmTransactionCommand(id = transactionId.toProto, confirmationCode = "42")
      )
    )
  }

   private def isNotFinished(state: TransactionView): Boolean =
    state.status.isCreated || state.status.isInProgress

  private def simulateUserActivity(logger: LogIO[UIO]): URIO[clock.Clock, Unit] = ???
}

```

### Building workflows with DIstage integration

If you're familiar with DIstage (which I strongly recommend to be familiar with!),  
defining Workflows looks pretty similar to defining Module:

```scala
// Library imports
import zio.temporal.distage._

// Protocol imports
import com.example.payments.impl._
import com.example.payments.workflows._

// Temporal Worker definition
object PaymentWorker extends ZWorkerDef("payments") {
  // Register activity inside this worker (with dependency injection)
  registerActivity[PaymentActivity].from[PaymentActivityImpl]

  // A little bit tricky to register workflows with parameters
  // (in this case - logstage logger)
  registerWorkflow[PaymentWorkflow].fromFactory((rootLogger: IzLogger) => () => new PaymentWorkflowImpl(rootLogger))
}
```

Then you can include the worker as a regular DIstage module!
```scala
import distage.ModuleDef


object ExampleModule extends ModuleDef {
   // Predefined modules from ZIO Temporal
   include(TemporalZioConfigModule)
   include(TemporalZioClientModule)
   include(TemporalZioWorkerModule)
  
   // Include your worker
   include(PaymentWorker)

   // Some Temporal client options, like serializers
   make[ZWorkflowClientOptions].fromValue(
      ZWorkflowClientOptions.default.withDataConverter(
         // ZIO Temporal can automatically find all your generated Protobuf files inside the Classpath!
         ScalapbDataConverter.makeAutoLoad()
      )
   )

   // External Workflow user!
   make[ExampleFlow]
}

```


### How to run examples

1. Start temporal server locally with predefined [docker-compose file](./examples/docker-compose.yaml):

```shell
docker-compose -f examples/docker-compose.yaml up -d
```

2. Run example from sbt:

```shell
sbt examples/run
```
