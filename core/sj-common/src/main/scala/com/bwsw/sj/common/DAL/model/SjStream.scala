package com.bwsw.sj.common.DAL.model

import com.bwsw.sj.common.rest.entities.stream.SjStreamData
import org.mongodb.morphia.annotations._

@Entity("streams")
class SjStream() {
  @Id var name: String = null
  var description: String = "No description"
  @Reference var service: Service = null
  @Property("stream-type") var streamType: String = null
  var tags: Array[String] = Array()

  def this(name: String,
           description: String,
           service: Service,
           streamType: String,
           tags: Array[String]) = {
    this()
    this.name = name
    this.description = description
    this.service = service
    this.streamType = streamType
    this.tags = tags
  }

  def create(): Unit = ???

  def delete(): Unit = ???

  def asProtocolStream(): SjStreamData = ???

  protected def fillProtocolStream(stream: SjStreamData) = {
    stream.name = this.name
    stream.description = this.description
    stream.service = this.service.name
    stream.streamType = this.streamType
    stream.tags = this.tags
  }
}
