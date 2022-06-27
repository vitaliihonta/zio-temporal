package zio.temporal.activity

import io.temporal.client.ActivityCompletionClient
import zio._
import zio.temporal.workflow.ZWorkflowClient

/** Represents options required to run the effects in the activity implementation
  */
class ZActivityOptions(val runtime: Runtime[ZEnv], val activityCompletionClient: ActivityCompletionClient)

object ZActivityOptions {

  /** Creates [[ZActivityOptions]]
    *
    * @param client
    *   workflow client
    * @return
    *   build activity options
    */
  def make(client: ZWorkflowClient): URIO[ZEnv, ZActivityOptions] =
    for {
      runtime                  <- ZIO.runtime[ZEnv]
      activityCompletionClient <- client.newActivityCompletionClient
    } yield new ZActivityOptions(runtime, activityCompletionClient)
}
