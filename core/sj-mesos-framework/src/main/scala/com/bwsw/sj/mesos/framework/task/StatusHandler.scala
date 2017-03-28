package com.bwsw.sj.mesos.framework.task

import com.bwsw.sj.mesos.framework.task.status._
import org.apache.log4j.Logger
import org.apache.mesos.Protos._

/**
  * Handler for mesos task status.
  */
object StatusHandler {
  val logger = Logger.getLogger(getClass)

  /**
    * Determine type of status and restart task, if status "failed" or "error"
    * @param status: mesos task status
    */
  def handle(status: TaskStatus) = {

    if (status != null) {

      TasksList(status.getTaskId.getValue).foreach(task => task.update(
        state = status.getState.toString,
        stateChanged = status.getTimestamp.toLong * 1000,
        lastNode = if (task.node != "") task.node else task.lastNode, node = status.getSlaveId.getValue
      ))

      logger.debug(s"Task: ${status.getTaskId.getValue}.")
      logger.info(s"Status: ${status.getState}.")

      status.getState.toString match {
        case "TASK_FAILED" | "TASK_ERROR" => FailureHandler.setStatus(status).process()
        case "TASK_RUNNING" => SuccessHandler.setStatus(status).process()
        case "TASK_KILLED" => KilledHandler.setStatus(status).process()
        case "TASK_LOST" => LostHandler.setStatus(status).process()
        case _ =>
      }
    }
  }
}








