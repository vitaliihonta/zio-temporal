package zio.temporal.internal

object SharedCompileTimeMessages {

  val wfMethodShouldntBeExtMethod: String =
    "Workflow method should not be an extension method!"

  val actMethodShouldntBeExtMethod: String =
    "Activity method should not be an extension method!"

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

  val generatedActivityExecute: String =
    "Generated activity execute"

  val generatedWorkflowExecute: String =
    "Generated workflow method execute"

  val generatedContinueAsNewExecute: String =
    "Generated continue as new execute"

  val generatedChildWorkflowExecuteAsync: String =
    "Generated child workflow execute async"

  val generatedActivityExecuteAsync: String =
    "Generated activity execute async"

  val generatedSignal: String =
    "Generated signal"

  val generatedSignalWithStart: String =
    "Generated signalWithStart"

  def notWorkflow(what: String): String =
    s"$what is not a workflow.\n" +
      s"Workflow interface must have @workflowInterface annotation.\n" +
      s"Hint: if the workflow type is used as a type parameter,\n" +
      s"add implicit IsWorkflow[$what] do the class or method definition"

  def usingNonStubOf(stubType: String, method: String, tpe: String): String =
    s"$stubType.$method must be used only with typed $stubType.Of[A],\n" +
      s"but $tpe found. Perhaps you added an explicit type annotation?\n" +
      s"The actual type must be $stubType.Of[$tpe]"

  def notActivity(what: String): String =
    s"$what is not an activity.\n" +
      s"Activity interface must have @activityInterface annotation.\n" +
      s"Hint: if the workflow type is used as a type parameter,\n" +
      s"add implicit IsActivity[$what] do the class or method definition"

  def notWorkflowMethod(what: String): String =
    s"The method is not a @workflowMethod: $what"

  def notSignalMethod(what: String): String =
    s"The method is not a @signalMethod: $what"

  def notQueryMethod(what: String): String =
    s"The method is not a @queryMethod: $what"

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

  case class TemporalMethodParameterIssue(name: String, issue: String)
  object TemporalMethodParameterIssue {
    private val noteMoreSpecificType =
      "It would likely cause an error because Temporal client requires more specific type information for deserialization"

    def erasedToJavaLangObject(name: String, tpe: String): TemporalMethodParameterIssue =
      TemporalMethodParameterIssue(
        name,
        issue = s"type `$tpe` will be erased to java.lang.Object in runtime!\n$noteMoreSpecificType\n" +
          s"Hint: if `$tpe` is a type parameter of the workflow interface, provide an upper-bound for the type parameter"
      )

    def isJavaLangObject(name: String): TemporalMethodParameterIssue =
      TemporalMethodParameterIssue(
        name,
        issue = s"type is java.lang.Object!\n$noteMoreSpecificType"
      )
  }

  def temporalMethodParameterTypesHasIssue(
    method: String,
    issue:  TemporalMethodParameterIssue
  ): String = {
    s"method $method parameter `${issue.name}` ${issue.issue}"
  }
}
