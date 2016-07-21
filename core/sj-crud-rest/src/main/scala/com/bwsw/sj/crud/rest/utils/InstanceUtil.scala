package com.bwsw.sj.crud.rest.utils

import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.ModuleConstants._
import com.bwsw.sj.crud.rest.runner.InstanceDestroyer
import org.slf4j.LoggerFactory

/**
  * Created: 20/07/2016
  *
  * @author Kseniya Tomskikh
  */
object InstanceUtil {
  private val logger = LoggerFactory.getLogger(this.getClass.getName)
  private val instanceDAO: GenericMongoService[Instance] = ConnectionRepository.getInstanceService

  def checkStatusInstances() = {
    logger.info("Run crud-rest. Check instances.")
    val instances = instanceDAO.getAll.filter { instance =>
      instance.status.equals(starting) ||
        instance.status.equals(stopping) ||
        instance.status.equals(deleting)
    }

    instances.foreach { instance =>
      new Thread(new InstanceDestroyer(instance, 1000)).start()
    }
  }

}
