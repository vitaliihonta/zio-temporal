package zio.temporal.internal

object SharedCompileTimeMessages {

  val wfMethodShouldntBeExtMethod: String =
    "Workflow method should not be an extension method!"

  val sgnlMethodShouldntBeExtMethod: String =
    "Signal method should not be an extension method!"

  val qrMethodShouldntBeExtMethod: String =
    "Query method should not be an extension method!"

  val generatedQueryInvoke: String =
    "Generated query invocation"

  val generatedWorkflowStart: String =
    "Generated workflow method start"

  val generateChildWorkflowExecute: String =
    "Generated child workflow execute"

  val generatedWorkflowExecute: String =
    "Generated workflow method execute"

  val generatedChildWorkflowExecuteAsync: String =
    "Generated child workflow start async"

  val generatedSignal: String =
    "Generated signal"

  val generatedSignalWithStart: String =
    "Generated signalWithStart"

  def notWorkflow(what: String): String =
    s"$what is not a workflow!"

  def notActivity(what: String): String =
    s"$what is not an activity!"

  def notWorkflowMethod(what: String): String =
    s"The method is not a @workflowMethod: $what"

  def notSignalMethod(what: String): String =
    s"The method is not a @signalMethod: $what"

  def notQueryMethod(what: String): String =
    s"The method is not a @queryMethod: $what"

  def notFound(what: String): String =
    s"$what not found"

  def methodArgumentsMismatch(
    name:       String,
    expected:   String,
    argumentNo: Int,
    actual:     String,
    actualTpe:  String
  ): String =
    s"Provided arguments for method $name doesn't confirm to it's signature:\n" +
      s"\tExpected: $expected (argument #${argumentNo + 1})\n" +
      s"\tGot: $actual (of type $actualTpe)"

  def defaultArgumentsNotSupported(names: List[String]): String =
    s"\nCurrently, methods with default arguments are not supported.\n" +
      s"Found the following default arguments: ${names.mkString(", ")}.\n" +
      s"Temporal doesn't work well with scala's implementation of default arguments, and throws the following error at runtime:\n" +
      s"[Just an example] java.lang.IllegalArgumentException: Missing @WorkflowMethod, @SignalMethod or @QueryMethod annotation on public default scala.Option zio.temporal.fixture.SignalWorkflow.getProgress$$default$$1()"

  def methodNotFound(instanceTpe: String, methodName: String, errorDetails: String): String =
    s"$instanceTpe doesn't have a $methodName method. $errorDetails"

  def expectedSimpleMethodInvocation(treeCls: Class[_], tree: String): String =
    s"Expected simple method invocation, got tree of class $treeCls: $tree"

  def unexpectedLibraryError(details: String): String =
    s"Unexpected library error! This should not normally happen.\n" +
      s"Please, fill up an issue on the GitHub." +
      s"\n$details"

  def isNotConcreteClass(tpe: String): String =
    s"$tpe should be a concrete (non-abstract) class"

  def shouldHavePublicNullaryConstructor(tpe: String): String =
    s"$tpe should have a public zero-argument constructor"
}
