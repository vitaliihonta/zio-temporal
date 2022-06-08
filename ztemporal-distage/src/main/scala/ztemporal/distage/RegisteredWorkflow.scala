package ztemporal.distage

/** '''WARNING''' Should not be used outside of [[ZWorkerDef]].
  *
  * Represents an workflow implementation being registered within [[ZWorkerDef]]
  */
class RegisteredWorkflow[A](
  private[ztemporal] val cls:        Class[A],
  private[ztemporal] val factoryOpt: Option[ZWorkflowFactory[A]])
