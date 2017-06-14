/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.bwsw.sj.crud.rest.model.instance.response

import com.bwsw.sj.common.dal.model.instance.{FrameworkStage, InputTask}

class InputInstanceApiResponse(moduleName: String,
                               moduleVersion: String,
                               moduleType: String,
                               stage: FrameworkStage,
                               status: String,
                               name: String,
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
                               engine: String,
                               restAddress: String,
                               val outputs: Array[String],
                               val checkpointMode: String,
                               val checkpointInterval: Long,
                               val duplicateCheck: Boolean,
                               val lookupHistory: Int,
                               val queueMaxSize: Int,
                               val defaultEvictionPolicy: String,
                               val evictionPolicy: String,
                               val backupCount: Int,
                               val asyncBackupCount: Int,
                               val tasks: Map[String, InputTask])
  extends InstanceApiResponse(
    moduleName,
    moduleVersion,
    moduleType,
    stage,
    status,
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
    engine,
    restAddress)
