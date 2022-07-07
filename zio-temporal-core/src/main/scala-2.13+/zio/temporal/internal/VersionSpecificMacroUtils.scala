package zio.temporal.internal

trait VersionSpecificMacroUtils { this: MacroUtils =>
  import c.universe._

  object NamedArgVersionSpecific {
    def unapply(namedArg: NamedArg): Option[(Tree, Tree)] =
      NamedArg.unapply(namedArg)
  }
}
