package com.bwsw.sj.engine.windowed.task.input

import com.bwsw.sj.common.DAL.model.SjStream
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.input.TaskInputService
import com.bwsw.sj.engine.core.entities.Envelope
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import org.slf4j.LoggerFactory

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * @author Kseniya Mikhaleva
 */
abstract class TaskInput[T <: Envelope](inputs: scala.collection.mutable.Map[SjStream, Array[Int]]) extends TaskInputService[T](inputs) {
  def get(): Iterable[T]
}

object TaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply(manager: CommonTaskManager) = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tStreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new CompleteTaskInput(manager)
      case (false, true) => new TStreamTaskInput(manager)
      case (true, false) => new KafkaTaskInput(manager)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new RuntimeException("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}