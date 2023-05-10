package com.example.hello.misc.generics

import zio.*
import zio.logging.backend.SLF4J
import zio.temporal.*
import zio.temporal.worker.{ZWorker, ZWorkerFactory, ZWorkerFactoryOptions}
import zio.temporal.workflow.*

trait WorkflowMixin {
  @workflowMethod
  def baseMethod(n: Int): Int
}

@workflowInterface
trait WorkflowFoo extends WorkflowMixin

class WorkflowFooImpl extends WorkflowFoo {
  override def baseMethod(n: Int): Int = n + 1
}

@workflowInterface
trait WorkflowBar extends WorkflowMixin

class WorkflowBarImpl extends WorkflowBar {
  override def baseMethod(n: Int): Int = n + 2
}

object GenericWorkflowsClientStubMain extends ZIOAppDefault {
  // Using base Workflow type here
  def basicExecute[W <: WorkflowMixin](stub: ZWorkflowStub.Of[W]): TemporalIO[Int] = {
    ZIO.logInfo("Executing generic workflows") *>
      ZWorkflowStub.execute(stub.baseMethod(40))
  }

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override val run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflow =
      ZWorkerFactory.newWorker("tmp") @@
        ZWorker.addWorkflow[WorkflowFoo].from(new WorkflowFooImpl) @@
        ZWorker.addWorkflow[WorkflowBar].from(new WorkflowBarImpl)

    val flow = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        wf1 <- client
                 .newWorkflowStub[WorkflowFoo]
                 .withTaskQueue("tmp")
                 .withWorkflowId("fooo")
                 .build

        wf2 <- client
                 .newWorkflowStub[WorkflowBar]
                 .withTaskQueue("tmp")
                 .withWorkflowId("baaar")
                 .build

        sum <- basicExecute(wf1).zipWith(basicExecute(wf2))(_ + _)

        _ <- ZIO.logInfo(s"Executed sum=$sum")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflow
      _ <- ZWorkflowServiceStubs.setup()
      _ <- ZWorkerFactory.setup
      _ <- flow
    } yield ()

    program
      .provideSome[Scope](
        // temporal
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make,
        // options
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make
      )
  }
}
