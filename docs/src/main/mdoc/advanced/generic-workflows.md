# Generic workflows
In order to define Workflow interfaces with type parameters, it's required to follow some guidelines.

Let's start with some basic imports that will be required for the whole demonstration:

```scala mdoc:silent
import zio._
import zio.temporal._
import zio.temporal.worker._
import zio.temporal.workflow._
import scala.reflect.ClassTag
```

## Ensure correct serialization setup
ZIO-Temporal is build on top of Java SDK and runs on JVM, which brings some limitations, such as [type erasure](https://docs.oracle.com/javase/tutorial/java/generics/erasure.html).  
Imagine a workflow interface with unconstrained type parameters:

```scala mdoc
@workflowInterface
trait BadGenericWorkflow[Input] {
  
  @workflowMethod
  def process(input: Input): Unit
}

def executeBadWorkflow[Input](
  stub: ZWorkflowStub.Of[BadGenericWorkflow[Input]]
)(input: Input): TemporalIO[Unit] =
  ZWorkflowStub.execute(
    stub.process(input)
  )
```

Invoking such a workflow will likely fail in runtime. Therefore, ZIO-Temporal provides a meaningful warning when executing such a workflow:
```
[warn] BadGenericWorkflow.scala:20:26: method process parameter `input` type `Input` will be erased to java.lang.Object in runtime!
[warn] It would likely cause an error because Temporal client requires more specific type information for deserialization
[warn] Hint: if `Input` is a type parameter of the workflow interface, provide an upper-bound for the type parameter
[warn]     ZWorkflowStub.execute(
```

To avoid possible issues:
1. Define a common parent type for possible workflow inputs
2. Specify the upper-bound type in the workflow interface


This is how proper jackson-serialized types look like
```scala mdoc
import com.fasterxml.jackson.annotation.{JsonSubTypes, JsonTypeInfo}

// NOTE: jackson (de)serialization won't work without additional annotations
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  Array(
    new JsonSubTypes.Type(value = classOf[Drink.Soda], name = "Soda"),
    new JsonSubTypes.Type(value = classOf[Drink.Juice], name = "Juice")
  )
)
sealed trait Drink
object Drink {
  case class Soda(kind: String)               extends Drink
  case class Juice(kind: String, volume: Int) extends Drink
}
```

## Define workflow interfaces properly
This is how the proper workflow looks like:

```scala mdoc:silent
// Intentionally missing the @workflowInterface annotation
trait DrinksWorkflows[DInput <: Drink] {

  @workflowMethod
  def process(input: DInput): Unit
}
```

Let's define 2 concrete workflow interfaces:

```scala mdoc:silent
@workflowInterface
trait SodaWorkflow extends DrinksWorkflows[Drink.Soda]

@workflowInterface
trait JuiceWorkflow extends DrinksWorkflows[Drink.Juice]
```

And simple implementations:
```scala mdoc:silent
class SodaWorkflowImpl extends SodaWorkflow {
  override def process(input: Drink.Soda): Unit =
    println(s"Simple soda: $input")
}

class JuiceWorkflowImpl extends JuiceWorkflow {
  override def process(input: Drink.Juice): Unit =
    println(s"Simple juice: $input")
}
```

Now they can be used easily:

```scala mdoc:silent
def executeWorkflow[DInput <: Drink](
  stub: ZWorkflowStub.Of[DrinksWorkflows[DInput]]
)(input: DInput): TemporalIO[Unit] =
  ZWorkflowStub.execute(
    stub.process(input)
  )

ZIO.serviceWithZIO[ZWorkflowClient] { client =>
  for {
    // Create stubs
    sodaWorkflow <- client
      .newWorkflowStub[SodaWorkflow]
      .withTaskQueue("generic")
      .withWorkflowId("soda-workflow-id")
      .build

    juiceWorkflow <- client
      .newWorkflowStub[JuiceWorkflow]
      .withTaskQueue("generic")
      .withWorkflowId("juice-workflow-id")
      .build
    
    _ <- executeWorkflow(sodaWorkflow)(Drink.Soda("Coke"))
    _ <- executeWorkflow(juiceWorkflow)(Drink.Juice("Orange", volume = 2))
  } yield ()
}
```

## Working with workflows as generics
Here goes a tricky example. Let's start with a simple parent workflow which will then run workflows above:

```scala mdoc:silent
// Intentionally missing the @workflowInterface annotation
trait DrinkFactory {
  @workflowMethod
  def processDrinks(count: Int): Unit
}
```
It's supposed to be 2 implementations of `DrinkFactory`: for `Soda` and `Juice`. They will both need to invoke corresponding `DrinkWorkflow`.  
In case you those workflows need to share some common logic, it could be extracted into an intermediate abstract class:

```scala mdoc:silent
abstract class ParallelDrinkFactory[
  DInput <: Drink,
  DWorkflow <: DrinksWorkflows[DInput]: IsWorkflow: ClassTag
] extends DrinkFactory {
  
  // Abstract method to be implemented by inheritors
  protected def produceDrinks(count: Int): List[DInput]
  
  override def processDrinks(count: Int): Unit = {
    ZAsync.foreachPar(produceDrinks(count)) { input =>
      // Polymorphically starts child workflows
      val child = ZWorkflow
        .newChildWorkflowStub[DWorkflow]
        .build
      
      ZChildWorkflowStub.executeAsync(
        child.process(input)
      )
    }.run.getOrThrow
  }
}
```

Having this abstract class, we can define concrete workflow interfaces:

```scala mdoc:silent
@workflowInterface
trait SodaFactory extends DrinkFactory

@workflowInterface
trait JuiceFactory extends DrinkFactory
```

And their implementations:

```scala mdoc:silent
class SodaFactoryImpl 
  extends ParallelDrinkFactory[Drink.Soda, SodaWorkflow]
  with SodaFactory {

  override protected def produceDrinks(count: Int): List[Drink.Soda] =
    List.tabulate(count)(n => Drink.Soda(s"Coke #$n"))
}

class JuiceFactoryImpl
  extends ParallelDrinkFactory[Drink.Juice, JuiceWorkflow]
    with JuiceFactory {

  override protected def produceDrinks(count: Int): List[Drink.Juice] =
    List.tabulate(count)(n => Drink.Juice(s"Juice #$n", volume = n))
}
```

**Notes**:
- In `ParallelDrinkFactory`, it's required to specify the upper-bound type for `DWorkflow`
- It's also required to have implicit `IsWorkflow` & `ClassTag`.
  - That's a witness that `DWorkflow` is actually a workflow interface. 
  - Without those instance, ZIO-Temporal won't compile the code because it is potentially unsafe

Example error:
```scala mdoc:fail
abstract class BadParentWorkflow[
  DInput <: Drink, 
  // Ask for ClassTag, but not for IsWorkflow 
  DWorkflow <: DrinksWorkflows[DInput]: ClassTag
] {
  
  // should not compile
  private val child = ZWorkflow.newChildWorkflowStub[DWorkflow].build
}
```
