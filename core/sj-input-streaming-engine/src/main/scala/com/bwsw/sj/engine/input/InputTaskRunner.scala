package com.bwsw.sj.engine.input

import java.util.concurrent.{ExecutorService, Executors}

import com.bwsw.sj.common.DAL.model.module.InputInstance
import com.bwsw.sj.engine.input.connection.tcp.server.InputStreamingServer
import com.google.common.util.concurrent.ThreadFactoryBuilder
import io.netty.buffer.{ByteBuf, Unpooled}
import org.slf4j.LoggerFactory

/**
 * Object is responsible for running a task of job that launches input module
 * Created: 07/07/2016
 *
 * @author Kseniya Mikhaleva
 */

object InputTaskRunner {

  val logger = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]) {

    val threadFactory = new ThreadFactoryBuilder()
      .setNameFormat("InputTaskRunner-%d")
      .setDaemon(true)
      .build()
    val executorService: ExecutorService = Executors.newFixedThreadPool(2, threadFactory)

    val buffer: ByteBuf = Unpooled.buffer()

    val manager: InputTaskManager = new InputTaskManager()
    logger.info(s"Task: ${manager.taskName}. Start preparing of task runner for input module\n")

    val inputInstanceMetadata = manager.getInstanceMetadata

    val inputTaskEngine = createInputTaskEngine(manager, inputInstanceMetadata)

    logger.info(s"Task: ${manager.taskName}. Preparing finished. Launch task\n")
    try {
      inputTaskEngine.runModule(executorService, buffer)
    } catch {
      case exception: Exception => {
        exception.printStackTrace()
        executorService.shutdownNow()
        System.exit(-1)
      }
    }

    new InputStreamingServer(manager.entryHost, manager.entryPort, buffer).run()
  }

  /**
   * Creates InputTaskEngine is in charge of a basic execution logic of task of input module
   * @param manager Manager of environment of task of input module
   * @param instance Input instance is a metadata for running a task of input module
   * @return Engine of input task
   */
  def createInputTaskEngine(manager: InputTaskManager, instance: InputInstance) = {
    instance.checkpointMode match {
      case "time-interval" =>
        logger.info(s"Task: ${manager.taskName}. Input module has a 'time-interval' checkpoint mode, create an appropriate task engine\n")
        logger.debug(s"Task: ${manager.taskName}. Create TimeCheckpointInputTaskEngine()\n")
        new TimeCheckpointInputTaskEngine(manager, instance)
      case "every-nth" =>
        logger.info(s"Task: ${manager.taskName}. Input module has a 'every-nth' checkpoint mode, create an appropriate task engine\n")
        new NumericalCheckpointInputTaskEngine(manager, instance)
    }
  }
}
