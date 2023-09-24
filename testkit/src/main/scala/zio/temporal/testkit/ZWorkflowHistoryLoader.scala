package zio.temporal.testkit

import io.temporal.testing.WorkflowHistoryLoader
import zio._
import zio.temporal.ZWorkflowExecutionHistory
import java.io.IOException
import java.nio.file.Path

/** Expose methods to read and deserialize workflow execution history from json.<br>
  *
  * <p>NOTE from Java SDK: 2021-11-29 Experimental because the user facing interface to history replay functionality is
  * actively evolving.
  */
object ZWorkflowHistoryLoader {

  /** Reads history from resource file
    */
  def readHistoryFromResource(resourceFileName: String): IO[IOException, ZWorkflowExecutionHistory] =
    ZIO.attemptBlockingIO {
      val history = WorkflowHistoryLoader.readHistoryFromResource(resourceFileName)
      new ZWorkflowExecutionHistory(history)
    }

  /** Reads history from a file
    */
  def readHistory(historyFile: Path): IO[IOException, ZWorkflowExecutionHistory] = {
    ZIO.attemptBlockingIO {
      val history = WorkflowHistoryLoader.readHistory(historyFile.toFile)
      new ZWorkflowExecutionHistory(history)
    }
  }
}
