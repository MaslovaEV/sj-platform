package com.bwsw.sj.crud.rest.utils

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model.headers.{ContentDispositionTypes, `Content-Disposition`}
import akka.http.scaladsl.model.{HttpEntity, HttpResponse, MediaTypes}
import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.rest.RestResponse
import com.bwsw.sj.crud.rest.CustomJar

/**
  * Provides methods for completion of sj-api response
  */
trait CompletionUtils {
  private val responseSerializer = new JsonSerializer()

  def restResponseToHttpResponse(restResponse: RestResponse): HttpResponse = {
    restResponse match {
      case customJar: CustomJar =>
        HttpResponse(
          headers = List(`Content-Disposition`(ContentDispositionTypes.attachment, Map("filename" -> customJar.filename))),
          entity = HttpEntity.Chunked.fromData(MediaTypes.`application/java-archive`, customJar.source)
        )
      case _ =>
        HttpResponse(
          status = restResponse.statusCode,
          entity = HttpEntity(`application/json`, responseSerializer.serialize(restResponse))
        )
    }
  }
}
