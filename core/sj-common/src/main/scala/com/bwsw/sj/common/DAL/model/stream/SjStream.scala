package com.bwsw.sj.common.DAL.model.stream

import com.bwsw.sj.common.DAL.model.service.Service
import com.bwsw.sj.common.DAL.morphia.MorphiaAnnotations.{IdField, PropertyField, ReferenceField}
import com.bwsw.sj.common.rest.entities.stream.StreamData
import org.mongodb.morphia.annotations._

@Entity("streams")
class SjStream(@IdField val name: String,
               val description: String,
               @ReferenceField val service: Service,
               val tags: Array[String],
               val force: Boolean,
               @PropertyField("stream-type") val streamType: String) {

  def create(): Unit = ???

  def delete(): Unit = ???

  def asProtocolStream(): StreamData = ???

  protected def fillProtocolStream(stream: StreamData) = {
    stream.name = this.name
    stream.description = this.description
    stream.service = this.service.name
    stream.streamType = this.streamType
    stream.tags = this.tags
    stream.force = this.force
  }
}
