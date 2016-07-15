package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.{Entity, Id, Property}

@Entity("services")
class Service() {
  @Id var name: String = null
  @Property("type") var serviceType: String = null
  var description: String = null

  def this(name: String, serviceType: String, description: String) = {
    this()
    this.name = name
    this.serviceType = serviceType
    this.description = description
  }
}