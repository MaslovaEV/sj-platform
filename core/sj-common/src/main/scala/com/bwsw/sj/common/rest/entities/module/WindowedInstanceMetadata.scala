package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.WindowedInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._
import com.bwsw.sj.common.utils.StreamLiterals._

class WindowedInstanceMetadata extends InstanceMetadata {
  var mainStream: String = null
  var relatedStreams: Array[String] = Array()
  var batchFillType: BatchFillType = null
  var window: Int = 1
  var slidingInterval: Int = 1
  var outputs: Array[String] = Array()
  var executionPlan: ExecutionPlan = new ExecutionPlan()
  var startFrom: String = EngineLiterals.newestStartMode
  var stateManagement: String = EngineLiterals.noneStateMode
  var stateFullCheckpoint: Int = 100
  var eventWaitIdleTime: Long = 1000

  override def asModelInstance() = {
    val modelInstance = new WindowedInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.mainStream = this.mainStream
    modelInstance.relatedStreams = this.relatedStreams
    modelInstance.batchFillType = this.batchFillType
    modelInstance.window = this.window
    modelInstance.slidingInterval = this.slidingInterval
    modelInstance.eventWaitIdleTime = this.eventWaitIdleTime
    modelInstance.stateManagement = this.stateManagement
    modelInstance.stateFullCheckpoint = this.stateFullCheckpoint
    modelInstance.outputs = this.outputs
    modelInstance.startFrom = this.startFrom
    modelInstance.executionPlan = this.executionPlan

    modelInstance
  }

  override def prepareInstance(moduleType: String,
                               moduleName: String,
                               moduleVersion: String,
                               engineName: String,
                               engineVersion: String) = {
    val clearInputs = inputsOrEmptyList().map(clearStreamFromMode)
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))

    val inputStreams = getStreams(clearInputs)
    val outputStreams = this.outputs
    val streams = inputStreams.filter(s => s.streamType.equals(tstreamType)).map(_.name).union(outputStreams)
    fillStages(streams)
  }

  override def createStreams() = {
    val inputs = inputsOrEmptyList()
    val sjStreams = getStreams(inputs.map(clearStreamFromMode) ++ this.outputs)
    sjStreams.foreach(_.create())
  }

  override def inputsOrEmptyList() = this.relatedStreams :+ this.mainStream
}
