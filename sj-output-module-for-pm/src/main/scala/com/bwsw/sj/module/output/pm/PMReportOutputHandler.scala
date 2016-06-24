package com.bwsw.sj.module.output.pm

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.engine.core.entities.{OutputEnvelope, TStreamEnvelope}
import com.bwsw.sj.engine.core.output.OutputStreamingHandler
import com.bwsw.sj.module.output.pm.data.PerformanceMetrics

/**
 * Handler for work with performance metrics t-stream envelopes
 *
 * Created: 23/06/2016
 *
 * @author Kseniya Mikhaleva
 */
class PMReportOutputHandler extends OutputStreamingHandler {
  val jsonSerializer = new JsonSerializer()

  /**
   * Transform t-stream transaction to output entities
   *
   * @param envelope Input T-Stream envelope
   * @return List of output envelopes
   */
  def onTransaction(envelope: TStreamEnvelope): List[OutputEnvelope] = {
    val list = envelope.data.map { rawPM =>
      val data: PerformanceMetrics = jsonSerializer.deserialize[PerformanceMetrics](new String(rawPM))
      val outputEnvelope = new OutputEnvelope
      outputEnvelope.data = data
      outputEnvelope.streamType = "elasticsearch-output"
      outputEnvelope
    }
    list
  }
}
