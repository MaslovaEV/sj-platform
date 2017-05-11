package com.bwsw.sj.engine.batch.task.input

import com.bwsw.sj.common.dal.model.stream.StreamDomain
import com.bwsw.sj.common.utils.StreamLiterals
import com.bwsw.sj.engine.core.engine.input.CheckpointTaskInput
import com.bwsw.sj.engine.core.entities.{Envelope, KafkaEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.managment.CommonTaskManager
import org.slf4j.{Logger, LoggerFactory}

/**
 * Class is responsible for handling an input streams of specific type(types),
 * i.e. for consuming, processing and sending the input envelopes
 *
 * @author Kseniya Mikhaleva
 */
abstract class RetrievableCheckpointTaskInput[T <: Envelope](val inputs: scala.collection.mutable.Map[StreamDomain, Array[Int]]) extends CheckpointTaskInput[T](inputs) {
  def get(): Iterable[T]
}

object RetrievableCheckpointTaskInput {
  private val logger = LoggerFactory.getLogger(this.getClass)

  def apply[T <: AnyRef](manager: CommonTaskManager): RetrievableCheckpointTaskInput[_ <: Envelope] = {
    val isKafkaInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.kafkaStreamType)
    val isTstreamInputExist = manager.inputs.exists(x => x._1.streamType == StreamLiterals.tstreamType)

    (isKafkaInputExist, isTstreamInputExist) match {
      case (true, true) => new RetrievableCompleteCheckpointTaskInput[T](manager)
      case (false, true) => new RetrievableTStreamCheckpointTaskInput[T](manager)
      case (true, false) => new RetrievableKafkaCheckpointTaskInput[T](manager)
      case _ =>
        logger.error("Type of input stream is not 'kafka' or 't-stream'")
        throw new RuntimeException("Type of input stream is not 'kafka' or 't-stream'")
    }
  }
}