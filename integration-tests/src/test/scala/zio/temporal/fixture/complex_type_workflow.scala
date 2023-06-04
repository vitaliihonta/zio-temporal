package zio.temporal.fixture

import zio._
import zio.temporal._
import zio.temporal.activity._
import zio.temporal.state.ZWorkflowState
import zio.temporal.workflow._

import java.util.UUID
import scala.concurrent.TimeoutException

case class Foo(bar: String)

case class Triple[A, B, C](first: A, second: B, third: C)

@activityInterface
trait ComplexTypesActivity {
  @activityMethod
  def complexList: List[Foo]

  @activityMethod
  def either: Either[String, Int]

  @activityMethod
  def triple: Triple[Foo, Int, String]

  @activityMethod
  def superNested: Either[List[String], Triple[Option[Int], Set[UUID], Boolean]]
}

case class ComplexTypesActivityImpl()(implicit options: ZActivityOptions[Any]) extends ComplexTypesActivity {
  override def either: Either[String, Int] =
    ZActivity.run {
      ZIO.succeed(Right(42))
    }

  override def complexList: List[Foo] =
    ZActivity.run {
      ZIO.succeed(List(Foo("x"), Foo("y")))
    }

  override def triple: Triple[Foo, Int, String] =
    ZActivity.run {
      ZIO.succeed(Triple(Foo("x"), 1, "y"))
    }

  override def superNested: Either[List[String], Triple[Option[Int], Set[UUID], Boolean]] = {
    Right(
      Triple(
        first = None,
        second = Set(
          UUID.fromString("8858bba1-3193-4c68-8e0e-5e29caeac210"),
          UUID.fromString("992858ce-58d1-4887-a7a3-6d3ee2f62c09")
        ),
        third = false
      )
    )
  }
}

@workflowInterface
trait EitherWorkflow {

  @workflowMethod
  def start: Either[String, Int]
}

case class EitherWorkflowImpl() extends EitherWorkflow {
  override def start: Either[String, Int] = {
    val stub = ZWorkflow
      .newActivityStub[ComplexTypesActivity]
      .withStartToCloseTimeout(5.seconds)
      .build

    ZActivityStub.execute(stub.either)
  }
}

@workflowInterface
trait ComplexWorkflow {
  @workflowMethod
  def start: Either[List[String], Triple[Option[Int], Set[UUID], Boolean]]

  @queryMethod
  def query1: List[Foo]

  @queryMethod
  def query2: Triple[Foo, Int, String]

  @signalMethod
  def resume(): Unit
}

class ComplexWorkflowImpl extends ComplexWorkflow {
  private val stub = ZWorkflow
    .newActivityStub[ComplexTypesActivity]
    .withStartToCloseTimeout(5.seconds)
    .build

  private val resumed = ZWorkflowState.empty[Unit]
  private val list    = ZWorkflowState.empty[List[Foo]]
  private val triple  = ZWorkflowState.empty[Triple[Foo, Int, String]]

  override def start: Either[List[String], Triple[Option[Int], Set[UUID], Boolean]] = {
    list := ZActivityStub.execute(
      stub.complexList
    )
    triple := ZActivityStub.execute(
      stub.triple
    )
    val completed = ZWorkflow.awaitWhile(5.seconds)(resumed.isEmpty)
    if (!completed) throw new TimeoutException()
    else {
      val result = ZActivityStub.executeAsync(
        stub.superNested
      )
      result.run.getOrThrow
    }
  }

  override def query1: List[Foo] =
    list.snapshot

  override def query2: Triple[Foo, Int, String] =
    triple.snapshot

  override def resume(): Unit = {
    resumed := ()
  }
}
