package zio.temporal.internal

trait VersionSpecificMacroUtils { this: MacroUtils =>
  import c.universe._

  object NamedArgVersionSpecific {
    def unapply(namedArg: AssignOrNamedArg): Option[(Tree, Tree)] =
      AssignOrNamedArg.unapply(namedArg)
  }
}
