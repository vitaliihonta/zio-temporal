package zio.temporal.activity

import io.temporal.client.ActivityCompletionClient
import zio._
import zio.temporal.workflow.ZWorkflowClient

/** Represents options required to run the effects in the activity implementation
  */
class ZActivityOptions[R](
  val runtime:                  Runtime[R],
  val activityCompletionClient: ActivityCompletionClient)

object ZActivityOptions {

  /** Creates [[ZActivityOptions]]
    *
    * @return
    *   build activity options
    */
  def make[R: Tag]: URLayer[R with ZWorkflowClient, ZActivityOptions[R]] =
    ZLayer.fromZIO {
      for {
        runtime                  <- ZIO.runtime[R]
        client                   <- ZIO.environment[ZWorkflowClient]
        activityCompletionClient <- client.get.newActivityCompletionClient
      } yield new ZActivityOptions(runtime, activityCompletionClient)
    }
}
