package zio.temporal.schedules

import io.temporal.client.schedules._
import io.temporal.common.interceptors.Header
import zio._
import zio.temporal.TemporalIO
import zio.temporal.internal.{ClassTagUtils, TemporalInteraction}
import zio.temporal.workflow.{IsWorkflow, ZWorkflowServiceStubs, ZWorkflowStubBuilderTaskQueueDsl}
import zio.stream._

import scala.reflect.ClassTag

/** Represents Temporal schedule client
  *
  * @see
  *   [[ScheduleClient]]
  */
final class ZScheduleClient private[zio] (val toJava: ScheduleClient) {

  /** Create a schedule and return its handle.
    *
    * @param scheduleId
    *   Unique ID for the schedule.
    * @param schedule
    *   Schedule to create.
    * @param options
    *   Options for creating the schedule.
    * @throws ScheduleAlreadyRunningException
    *   if the schedule is already running.
    * @return
    *   A handle that can be used to perform operations on a schedule.
    */
  def createSchedule(
    scheduleId: String,
    schedule:   ZSchedule,
    options:    ZScheduleOptions = ZScheduleOptions.default
  ): TemporalIO[ZScheduleHandle] =
    TemporalInteraction.from {
      new ZScheduleHandle(
        toJava.createSchedule(scheduleId, schedule.toJava, options.toJava)
      )
    }

  /** Gets the schedule handle for the given ID.
    *
    * @param scheduleId
    *   Schedule ID to get the handle for.
    * @return
    *   A handle that can be used to perform operations on a schedule.
    */
  def getHandle(scheduleId: String): TemporalIO[ZScheduleHandle] =
    TemporalInteraction.from {
      new ZScheduleHandle(
        toJava.getHandle(scheduleId)
      )
    }

  /** List schedules.
    *
    * @param pageSize
    *   how many results to fetch from the Server at a time. Optional, default is 100.
    * @return
    *   sequential stream that performs remote pagination under the hood
    */
  def listSchedules(pageSize: Option[Int] = None): Stream[Throwable, ZScheduleListDescription] =
    ZStream
      .blocking(
        ZStream.fromJavaStreamZIO(
          ZIO.attempt(
            toJava.listSchedules(pageSize.map(Integer.valueOf).orNull)
          )
        )
      )
      .map(new ZScheduleListDescription(_))

  /** Creates new typed schedule start workflow stub builder. The instance could then be used to start a scheduled
    * workflow.
    *
    * @tparam A
    *   workflow interface
    * @param header
    *   headers sent with each workflow scheduled
    * @return
    *   builder instance
    */
  def newScheduleStartWorkflowStub[A: ClassTag: IsWorkflow](
    header: Header = Header.empty()
  ): ZWorkflowStubBuilderTaskQueueDsl[ZScheduleStartWorkflowStub.Of[A]] =
    new ZWorkflowStubBuilderTaskQueueDsl[ZScheduleStartWorkflowStub.Of[A]](options =>
      ZScheduleStartWorkflowStub.Of(
        new ZScheduleStartWorkflowStubImpl(
          ClassTagUtils.classOf[A],
          options,
          header
        )
      )
    )

  /** Creates new untyped schedule start workflow stub builder. The instance could then be used to start a scheduled
    * workflow.
    *
    * @param workflowType
    *   workflow type
    * @param header
    *   headers sent with each workflow scheduled
    * @return
    *   builder instance
    */
  def newUntypedScheduleStartWorkflowStub(
    workflowType: String,
    header:       Header = Header.empty()
  ): ZWorkflowStubBuilderTaskQueueDsl[ZScheduleStartWorkflowStub.Untyped] =
    new ZWorkflowStubBuilderTaskQueueDsl[ZScheduleStartWorkflowStub.Untyped](options =>
      new ZScheduleStartWorkflowStub.UntypedImpl(
        workflowType,
        options,
        header
      )
    )
}

object ZScheduleClient {

  /** Creates [[ZScheduleClient]] instance
    * @see
    *   [[ScheduleClient]]
    */
  val make: URLayer[ZWorkflowServiceStubs with ZScheduleClientOptions, ZScheduleClient] =
    ZLayer.fromZIO {
      ZIO.environmentWithZIO[ZWorkflowServiceStubs with ZScheduleClientOptions] { environment =>
        ZIO.succeedBlocking {
          new ZScheduleClient(
            ScheduleClient.newInstance(
              environment.get[ZWorkflowServiceStubs].toJava,
              environment.get[ZScheduleClientOptions].toJava
            )
          )
        }
      }
    }
}
