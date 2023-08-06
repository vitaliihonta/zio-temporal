package com.example.versioning

import io.temporal.client.BuildIdOperation
import zio.{BuildInfo => _, _}
import zio.cli._
import zio.logging.backend.SLF4J
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker._
import zio.temporal.workflow.{
  ZWorkflowClient,
  ZWorkflowClientOptions,
  ZWorkflowServiceStubs,
  ZWorkflowServiceStubsOptions,
  ZWorkflowStub
}
import zio.temporal.build.BuildInfo

object VersionedWorkflowMain extends ZIOCliDefault {

  val TaskQueue = "versioned-workflows"

  sealed trait Subcommand extends Product with Serializable
  object Subcommand {
    final case class Worker(buildId: String) extends Subcommand
    final case class Start(buildId: String)  extends Subcommand
  }

  private val worker = Command("worker", Options.none, Args.text("build-id"))
    .withHelp(HelpDoc.p("Run the worker with the specified build-id"))
    .map(Subcommand.Worker(_))

  private val start = Command("start", Options.none, Args.text("build-id"))
    .withHelp(HelpDoc.p("Start a subscription workflow"))
    .map(Subcommand.Start(_))

  private val example = Command("example", Options.none, Args.none)
    .subcommands(worker, start)

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val cliApp = CliApp.make(
    name = "Versioned workflow example",
    version = BuildInfo.version,
    summary = HelpDoc.Span.text("Runs the versioned workflow example"),
    command = example
  ) {
    case Subcommand.Start(buildId)  => startWorkflow(buildId)
    case Subcommand.Worker(buildId) => runWorker(buildId)
  }

  private def runWorker(buildId: String): ZIO[Scope, Exception, Unit] = {
    val workerOptions = ZWorkerOptions.default.withBuildId(buildId)

    val registerWorkflows = ZWorkerFactory.newWorker(TaskQueue, workerOptions) @@
      ZWorker.addWorkflow[SubscriptionWorkflowV1].fromClass @@
      ZWorker.addActivityImplementationService[SubscriptionActivities]

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkerFactory.serve
    } yield ()

    program.provideSome[Scope](
      SubscriptionActivitiesImpl.make,
      // temporal
      ZWorkflowClientOptions.make,
      ZWorkerFactoryOptions.make,
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make,
      ZWorkflowServiceStubsOptions.make,
      ZWorkerFactory.make,
      ZActivityOptions.default
    )
  }

  private def startWorkflow(buildId: String): ZIO[Any, Exception, Unit] = {
    val program = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        // starter should set the version
        _ <- client.updateWorkerBuildIdCompatibility(
               TaskQueue,
               // todo: figure out how it works
               BuildIdOperation.newIdInNewDefaultSet(buildId)
             )
        subscriptionId <- Random.nextUUID
        subscriptionWorkflow <- client
                                  .newWorkflowStub[SubscriptionWorkflow]
                                  .withTaskQueue(TaskQueue)
                                  .withWorkflowId(subscriptionId.toString)
                                  .build

        _ <- ZIO.logInfo(s"Starting subscription workflow id=$subscriptionId")
        execution <- ZWorkflowStub.start(
                       subscriptionWorkflow.proceedRecurringSubscription(subscriptionId.toString)
                     )
        _ <- ZIO.logInfo(s"Workflow started! Execution=$execution")
      } yield ()
    }

    program.provide(
      ZWorkflowServiceStubsOptions.make,
      ZWorkflowClientOptions.make,
      ZWorkflowClient.make,
      ZWorkflowServiceStubs.make
    )
  }
}
