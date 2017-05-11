package com.bwsw.sj.common.dal.model.stream

import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.model.service.ESServiceDomain
import com.bwsw.sj.common.rest.model.stream.ESStreamApi
import com.bwsw.sj.common.utils.{RestLiterals, StreamLiterals}

class ESStreamDomain(override val name: String,
                     override val service: ESServiceDomain,
                     override val description: String = RestLiterals.defaultDescription,
                     override val force: Boolean = false,
                     override val tags: Array[String] = Array())
  extends StreamDomain(name, description, service, force, tags, StreamLiterals.esOutputType) {

  override def asProtocolStream() =
    new ESStreamApi(
      name = name,
      service = service.name,
      tags = tags,
      force = force,
      description = description
    )

  override def delete(): Unit = {
    val hosts = this.service.provider.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet
    val client = new ElasticsearchClient(hosts)
    client.deleteDocuments(this.service.index, this.name)

    client.close()
  }
}
