# FAQ

## ZIO-Temporal uses Scala macros heavily. How to inspect the generated code?
Add the following VM parameter to SBT:  
```
-Dzio.temporal.debug.macro=true
```

It will print the generated code. For example:  
```shell
[info] -- Info: /Users/vitaliihonta/IdeaProjects/zio-temporal/integration-tests/src/test/scala/zio/temporal/WorkflowSpec.scala:213:25 
[info] 213 |        thirdSnapshot <- ZWorkflowStub.query(
[info]     |                         ^
[info]     |Generated query invocation tree=_root_.zio.temporal.internal.TemporalInteraction.from[scala.collection.immutable.List[scala.Predef.String]](zio.temporal.internal.TemporalWorkflowFacade.query[scala.collection.immutable.List[scala.Predef.String]](workflowStub.toJava, "messages", scala.Nil)(ctg$proxy8))
[info] 214 |                           workflowStub.messages
[info] 215 |                         )
```
