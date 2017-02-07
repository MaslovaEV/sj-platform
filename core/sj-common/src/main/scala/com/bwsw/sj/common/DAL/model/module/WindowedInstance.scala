package com.bwsw.sj.common.DAL.model.module

import com.bwsw.sj.common.rest.entities.module.{BatchFillType, ExecutionPlan, InstanceMetadata, WindowedInstanceMetadata}
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._
import org.mongodb.morphia.annotations.{Embedded, Property}

/**
 * Entity for windowed instance-json
 *
 * @author Kseniya Tomskikh
 */
class WindowedInstance() extends Instance {
  @Property("main-stream") var mainStream: String = null
  @Property("related-streams") var relatedStreams: Array[String] = Array()
  @Property("batch-fill-type") var batchFillType: BatchFillType = new BatchFillType()
  var window: Int = 1
  @Property("sliding-interval") var slidingInterval: Int = 1
  @Embedded("execution-plan") var executionPlan: ExecutionPlan = new ExecutionPlan()
  @Property("start-from") var startFrom: String = EngineLiterals.newestStartMode
  @Property("state-management") var stateManagement: String = EngineLiterals.noneStateMode
  @Property("state-full-checkpoint") var stateFullCheckpoint: Int = 100
  @Property("event-wait-idle-time") var eventWaitIdleTime: Long = 1000

  override def asProtocolInstance(): InstanceMetadata = {
    val protocolInstance = new WindowedInstanceMetadata()
    super.fillProtocolInstance(protocolInstance)

    protocolInstance.mainStream = this.mainStream
    protocolInstance.relatedStreams = this.relatedStreams
    protocolInstance.batchFillType = this.batchFillType
    protocolInstance.window = this.window
    protocolInstance.slidingInterval = this.slidingInterval
    protocolInstance.eventWaitIdleTime = this.eventWaitIdleTime
    protocolInstance.executionPlan = this.executionPlan
    protocolInstance.stateManagement = this.stateManagement
    protocolInstance.stateFullCheckpoint = this.stateFullCheckpoint
    protocolInstance.outputs = this.outputs
    protocolInstance.startFrom = this.startFrom

    protocolInstance
  }

  override def getInputsWithoutStreamMode() = (this.relatedStreams :+ this.mainStream).map(clearStreamFromMode)
}
