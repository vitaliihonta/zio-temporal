package zio.temporal.activity

import io.temporal.client.ActivityCompletionClient
import zio._
import zio.temporal.workflow.ZWorkflowClient

/** Represents options required to run the effects in the activity implementation
  */
class ZActivityOptions[+R](
  val runtime: Runtime[R],
  // activity test environment doesn't support async completion
  private[zio] val activityCompletionClientOpt: Option[ActivityCompletionClient])

object ZActivityOptions {

  val default: URLayer[ZWorkflowClient, ZActivityOptions[Any]] =
    make[Any]

  /** Creates [[ZActivityOptions]]
    *
    * @return
    *   build activity options
    */
  def make[R: Tag]: URLayer[R with ZWorkflowClient, ZActivityOptions[R]] =
    ZLayer.fromZIO {
      for {
        runtime                  <- ZIO.runtime[R]
        client                   <- ZIO.service[ZWorkflowClient]
        activityCompletionClient <- client.newActivityCompletionClient
      } yield new ZActivityOptions(runtime, Some(activityCompletionClient))
    }
}
