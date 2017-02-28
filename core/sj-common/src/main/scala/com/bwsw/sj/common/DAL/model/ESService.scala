package com.bwsw.sj.common.DAL.model

import com.bwsw.common.ElasticsearchClient
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.rest.entities.service.{EsServiceData, ServiceData}
import com.bwsw.sj.common.utils.ServiceLiterals
import org.mongodb.morphia.annotations.Reference

class ESService() extends Service {
  serviceType = ServiceLiterals.elasticsearchType
  @Reference var provider: Provider = null
  var index: String = null
  var login: String = null
  var password: String = null

  def this(name: String, serviceType: String, description: String, provider: Provider, index: String, login: String, password: String) = {
    this()
    this.name = name
    this.serviceType = serviceType
    this.description = description
    this.provider = provider
    this.index = index
    this.login = login
    this.password = password
  }

  override def asProtocolService(): ServiceData = {
    val protocolService = new EsServiceData()
    super.fillProtocolService(protocolService)

    protocolService.index = this.index
    protocolService.provider = this.provider.name
    protocolService.login = this.login
    protocolService.password = this.password

    protocolService
  }

  override def prepare() = {
    val client = new ElasticsearchClient(getProviderHosts())

    if (!client.doesIndexExist(this.index)) {
      client.createIndex(this.index)
    }

    client.close()
  }

  override def destroy() = {
    if (!isIndexUsed) {
      val client = new ElasticsearchClient(getProviderHosts())
      client.deleteIndex(this.index)
      client.close()
    }
  }

  private def getProviderHosts() = {
    this.provider.hosts.map(address => {
      val hostAndPort = address.split(":")
      val host = hostAndPort(0)
      val port = hostAndPort(1).toInt

      (host, port)
    }).toSet
  }

  private def isIndexUsed = {
    ConnectionRepository.getServiceManager.getByParameters(Map("type" -> this.serviceType))
      .map(x => x.asInstanceOf[ESService])
      .exists(x => x.index == this.index && x.name != this.name)
  }
}
