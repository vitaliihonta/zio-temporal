package zio.temporal.worker

import zio.temporal.activity.ZActivityOptions

// todo: implement
final case class ZWorkflowImplementationOptions private (
  defaultActivityOptions: ZActivityOptions) {}
