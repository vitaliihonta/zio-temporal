package ztemporal.distage

import ztemporal.internalApi

/** '''WARNING''' Should not be used outside of [[ZWorkerDef]].
  *
  * Represents an activity implementation being registered within [[ZWorkerDef]]
  */
class RegisteredActivity[A] @internalApi() (private[ztemporal] val activity: A)
