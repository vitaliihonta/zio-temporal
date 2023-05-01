package zio.temporal.fixture

import io.temporal.client.ActivityCompletionException
import zio.*
import zio.temporal.*
import zio.temporal.activity.*
import zio.temporal.workflow.*
import zio.temporal.state.*

import java.util.concurrent.atomic.AtomicInteger

@activityInterface
trait FibonacciHeartbeatActivity {
  def fibonacciSum(n: Int): BigDecimal
}

object FibonacciHeartbeatActivityImpl {
  case class HeartbeatDetails(sum: BigDecimal, curr: Int, prev: Int, step: Int)
}

class FibonacciHeartbeatActivityImpl(iterationsCounter: AtomicInteger)(implicit options: ZActivityOptions[Any])
    extends FibonacciHeartbeatActivity {
  import FibonacciHeartbeatActivityImpl.HeartbeatDetails

  override def fibonacciSum(n: Int): BigDecimal =
    ZActivity.run {
      val context = ZActivity.executionContext

      def iter(sum: BigDecimal, curr: Int, prev: Int, step: Int): IO[ActivityCompletionException, BigDecimal] =
        if (step >= n) ZIO.succeed(sum)
        else
          for {
            _ <- context.heartbeat(HeartbeatDetails(sum, curr, prev, step))
            _ <- ZIO.logInfo("Sleep...")
            _ <- ZIO.sleep((50 * step).millis)
            _       = iterationsCounter.incrementAndGet()
            newStep = step + 1
            newPrev = curr
            newCurr = curr + prev
            newSum  = sum + curr
            res <- iter(newSum, newCurr, newPrev, newStep)
          } yield res

      for {
        heartbeatDetails <- context.getHeartbeatDetails[HeartbeatDetails]
        progress = heartbeatDetails.getOrElse(
                     HeartbeatDetails(sum = 0, curr = 1, prev = 0, step = 0)
                   )
        res <- iter(progress.sum, progress.curr, progress.prev, progress.step)
      } yield res
    }
}
