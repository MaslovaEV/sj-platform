package com.bwsw.sj.common.DAL.model

import org.mongodb.morphia.annotations.Reference

class Generator() {
  var generatorType: String = null
  @Reference var service: Service = null
  var instanceCount: Int = 0
}
