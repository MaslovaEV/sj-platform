package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.service.RestServiceData
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.Reference

/**
  * Service for RESTful output.
  *
  * @author Pavel Tomskikh
  */
class RestService extends Service {
  serviceType = ServiceLiterals.restType
  @Reference var provider: Provider = _
  var basePath: String = _
  var httpVersion: String = _

  def this(
      name: String,
      serviceType: String,
      description: String,
      provider: Provider,
      basePath: String,
      httpVersion: String) = {
    this
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.basePath = basePath
    this.httpVersion = httpVersion
  }

  override def asProtocolService = {
    val protocolService = new RestServiceData
    super.fillProtocolService(protocolService)
    protocolService.provider = provider.name
    protocolService.basePath = basePath
    protocolService.httpVersion = httpVersion

    protocolService
  }
}
