package com.bwsw.sj.common.si.model.instance

import com.bwsw.sj.common.dal.model.instance.{ExecutionPlan, FrameworkStage}
import com.bwsw.sj.common.utils.EngineLiterals
import org.apache.avro.Schema

class BatchInstance(name: String,
                    description: String,
                    parallelism: Any,
                    options: Map[String, Any],
                    perTaskCores: Double,
                    perTaskRam: Int,
                    jvmOptions: Map[String, String],
                    nodeAttributes: Map[String, String],
                    coordinationService: String,
                    environmentVariables: Map[String, String],
                    performanceReportingInterval: Long,
                    moduleName: String,
                    moduleVersion: String,
                    moduleType: String,
                    engine: String,
                    val inputs: Array[String],
                    val outputs: Array[String],
                    val window: Int = 1,
                    val slidingInterval: Int = 1,
                    val startFrom: String = EngineLiterals.newestStartMode,
                    val stateManagement: String = EngineLiterals.noneStateMode,
                    val stateFullCheckpoint: Int = 100,
                    val eventWaitTime: Long = 1000,
                    val inputAvroSchema: Option[Schema] = None,
                    val executionPlan: ExecutionPlan = new ExecutionPlan(),
                    restAddress: Option[String] = None,
                    stage: FrameworkStage = FrameworkStage(),
                    status: String = EngineLiterals.ready)
  extends Instance(
    name,
    description,
    parallelism,
    options,
    perTaskCores,
    perTaskRam,
    jvmOptions,
    nodeAttributes,
    coordinationService,
    environmentVariables,
    performanceReportingInterval,
    moduleName,
    moduleVersion,
    moduleType,
    engine,
    restAddress,
    stage,
    status) {

}