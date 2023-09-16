package zio.temporal.activity

import io.temporal.client.ActivityCompletionClient
import zio._
import zio.temporal.workflow.ZWorkflowClient

/** Represents options required to run the effects in the activity implementation
  */
class ZActivityRunOptions[+R](
  val runtime: Runtime[R],
  // activity test environment doesn't support async completion
  private[zio] val activityCompletionClientOpt: Option[ActivityCompletionClient])

object ZActivityRunOptions {

  val default: URLayer[ZWorkflowClient, ZActivityRunOptions[Any]] =
    make[Any]

  /** Creates [[ZActivityRunOptions]]
    *
    * @return
    *   build activity options
    */
  def make[R: Tag]: URLayer[R with ZWorkflowClient, ZActivityRunOptions[R]] =
    ZLayer.fromZIO {
      for {
        runtime                  <- ZIO.runtime[R]
        client                   <- ZIO.service[ZWorkflowClient]
        activityCompletionClient <- client.newActivityCompletionClient
      } yield new ZActivityRunOptions(runtime, Some(activityCompletionClient))
    }
}
