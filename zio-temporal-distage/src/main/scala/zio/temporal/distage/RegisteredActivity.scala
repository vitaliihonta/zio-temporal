package zio.temporal.distage

import zio.temporal.internalApi

/** '''WARNING''' Should not be used outside of [[ZWorkerDef]].
  *
  * Represents an activity implementation being registered within [[ZWorkerDef]]
  */
class RegisteredActivity[A] @internalApi() (private[zio] val activity: A)
