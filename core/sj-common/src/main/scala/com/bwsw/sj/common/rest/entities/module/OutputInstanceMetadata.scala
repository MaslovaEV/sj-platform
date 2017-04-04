package com.bwsw.sj.common.rest.entities.module

import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.SjStreamUtils._

class OutputInstanceMetadata extends InstanceMetadata {
  var checkpointMode: String = null
  var checkpointInterval: Long = Long.MinValue
  var executionPlan: ExecutionPlan = new ExecutionPlan()
  var startFrom: String = EngineLiterals.newestStartMode
  var input: String = null
  var output: String = null

  override def asModelInstance() = {
    val modelInstance = new OutputInstance()
    super.fillModelInstance(modelInstance)
    modelInstance.checkpointMode = this.checkpointMode
    modelInstance.checkpointInterval = this.checkpointInterval
    modelInstance.inputs = Array(this.input)
    modelInstance.outputs = Array(this.output)
    modelInstance.startFrom = this.startFrom
    modelInstance.executionPlan = this.executionPlan

    modelInstance
  }

  override def prepareInstance(moduleType: String,
                               moduleName: String,
                               moduleVersion: String,
                               engineName: String,
                               engineVersion: String) = {
    val clearInputs = Array(clearStreamFromMode(this.input))
    super.prepareInstance(moduleType, moduleName, moduleVersion, engineName, engineVersion)
    castParallelismToNumber(getStreamsPartitions(clearInputs))
    this.executionPlan.fillTasks(createTaskStreams(), createTaskNames(this.parallelism.asInstanceOf[Int], this.name))
  }

  override def createStreams() = {
    val sjStreams = getStreams(Array(clearStreamFromMode(this.input)))
    sjStreams.foreach(_.create())
  }

  override def inputsOrEmptyList() = Array(this.input)
}
