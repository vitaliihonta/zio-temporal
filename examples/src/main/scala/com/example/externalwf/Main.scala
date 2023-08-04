package com.example.externalwf

import zio._
import zio.logging.backend.SLF4J
import zio.temporal._
import zio.temporal.activity.ZActivityOptions
import zio.temporal.worker._
import zio.temporal.workflow._

object Main extends ZIOAppDefault {
  val TaskQueue = "hello-food-workflows"

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] =
    Runtime.removeDefaultLoggers ++ SLF4J.slf4j

  override def run: ZIO[ZIOAppArgs with Scope, Any, Any] = {
    val registerWorkflows =
      ZWorkerFactory.newWorker(TaskQueue) @@
        ZWorker.addWorkflow[FoodDeliveryWorkflowImpl].fromClass @@
        ZWorker.addWorkflow[FoodOrderWorkflowImpl].fromClass

    val invokeWorkflows = ZIO.serviceWithZIO[ZWorkflowClient] { client =>
      for {
        workflowId <- Random.nextUUID
        orderWorkflow <- client
                           .newWorkflowStub[FoodOrderWorkflow]
                           .withTaskQueue(TaskQueue)
                           .withWorkflowId(workflowId.toString)
                           .withSearchAttributes(
                             /** NOTE: make sure to add a search attributes
                               * {{{
                               *    temporal operator search-attribute create --namespace default --name Vendor --type Keyword
                               *    temporal operator search-attribute create --namespace default --name VendorVersion --type Text
                               * }}}
                               */
                             Map(
                               "Vendor"        -> ZSearchAttribute.keyword("vhonta.dev"),
                               "VendorVersion" -> ZSearchAttribute("1.0.0")
                             )
                           )
                           .build

        deliveryWorkflow <- client
                              .newWorkflowStub[FoodDeliveryWorkflow]
                              .withTaskQueue(TaskQueue)
                              .withWorkflowId(FoodDeliveryWorkflow.makeId(workflowId.toString))
                              .build

        goods           = List("peperoni pizza", "coke")
        deliveryAddress = "Sample street, 5/2, 10000"
        _ <- ZIO.logInfo("Starting order & delivery workflow!")
        // Restaurant & delivery are kinda independent
        // and can be re-tried independently
        // That's why it's independent workflows
        _ <- ZIO.collectAllParDiscard(
               List(
                 ZWorkflowStub.start(
                   orderWorkflow.order(goods, deliveryAddress)
                 ),
                 ZWorkflowStub.start(
                   deliveryWorkflow.deliver(goods)
                 )
               )
             )
        _ <- userInteraction(orderWorkflow)
        finished <- ZIO
                      .whenZIO(orderWorkflow.result[Boolean])(ZIO.succeed("finished"))
                      .someOrElse("not finished")
        _ <- ZIO.logInfo(s"The order was $finished")
      } yield ()
    }

    val program = for {
      _ <- registerWorkflows
      _ <- ZWorkerFactory.setup
      _ <- ZWorkflowServiceStubs.setup()
      _ <- invokeWorkflows
    } yield ()

    program
      .provideSome[Scope](
        ZWorkflowServiceStubsOptions.make,
        ZWorkflowClientOptions.make,
        ZWorkerFactoryOptions.make,
        ZWorkflowClient.make,
        ZWorkflowServiceStubs.make,
        ZWorkerFactory.make
      )
  }

  private def userInteraction(orderWorkflow: ZWorkflowStub.Of[FoodOrderWorkflow]): TemporalIO[Unit] = {
    val confirmPayed =
      ZIO.logInfo("Paying the order...") *>
        ZWorkflowStub.signal(
          orderWorkflow.confirmPayed()
        )

    val cancelOrder =
      ZIO.logInfo("Cancelling the order...") *>
        ZWorkflowStub.signal(
          orderWorkflow.cancelOrder()
        )

    val doNothing =
      ZIO.logInfo("Oh, I forgot about my iron!").unit

    for {
      _ <- ZIO.logInfo("User is thinking what to do...")
      _ <- ZIO.sleep(20.seconds)
      /** 3 options to do
        *   - [[confirmPayed]], an external signal with parameters will be sent
        *   - [[cancelOrder]], a different signal will be sent
        *   - [[doNothing]], order will be cancelled by timeout
        */
      _ <- confirmPayed
    } yield ()
  }
}
