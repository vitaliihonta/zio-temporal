package zio.temporal.distage

/** '''WARNING''' Should not be used outside of [[ZWorkerDef]].
  *
  * Represents an workflow implementation being registered within [[ZWorkerDef]]
  */
class RegisteredWorkflow[A](
  private[zio] val cls:        Class[A],
  private[zio] val factoryOpt: Option[ZWorkflowFactory[A]])
