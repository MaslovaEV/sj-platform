package com.bwsw.sj.engine.output.task

import com.bwsw.sj.common.DAL.model.module.OutputInstance
import com.bwsw.sj.common.engine.DefaultEnvelopeDataSerializer
import com.bwsw.sj.engine.core.entities.EsEnvelope
import com.bwsw.sj.engine.core.environment.{EnvironmentManager, ModuleEnvironmentManager, OutputEnvironmentManager}
import com.bwsw.sj.engine.core.managment.TaskManager
import com.bwsw.sj.engine.core.output.OutputStreamingExecutor

/**
  * Task manager for working with streams of output-streaming module
  *
  * @author Kseniya Tomskikh
  */
class OutputTaskManager() extends TaskManager {
  private val executorInstance = executorClass.getConstructor(classOf[ModuleEnvironmentManager])
    .newInstance(new EnvironmentManager(Map(), Array()))
  val _type = executorInstance.getClass.getMethod("getType").invoke(executorInstance).asInstanceOf[_root_.scala.reflect.runtime.universe.Type]
  lazy val envelopeDataSerializer = new DefaultEnvelopeDataSerializer[_type.type]()
  val outputInstance = instance.asInstanceOf[OutputInstance]
  val inputs = getInputs(outputInstance.executionPlan)

  require(numberOfAgentsPorts >= 2, "Not enough ports for t-stream consumers/producers ")

  def getExecutor(environmentManager: EnvironmentManager) = {
    logger.debug(s"Task: $taskName. Start loading of executor class from module jar.")
    val executor = executorClass.getConstructor(classOf[OutputEnvironmentManager])
      .newInstance(environmentManager)
      .asInstanceOf[OutputStreamingExecutor[_type.type]]
    logger.debug(s"Task: $taskName. Create an instance of executor class.")

    executor
  }

  def getOutputModuleEntity(): EsEnvelope = {
    logger.info(s"Task: $taskName. Getting entity object from jar of file: " +
      instance.moduleType + "-" + instance.moduleName + "-" + instance.moduleVersion + ".")
    val entityClassName = fileMetadata.specification.entityClass
    val outputEntity = moduleClassLoader
      .loadClass(entityClassName)
      .newInstance()
      .asInstanceOf[EsEnvelope]

    outputEntity
  }
}
