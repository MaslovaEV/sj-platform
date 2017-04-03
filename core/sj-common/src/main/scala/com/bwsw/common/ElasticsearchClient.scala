package com.bwsw.common

import java.net.InetAddress
import java.util.UUID

import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.InetSocketTransportAddress
import org.elasticsearch.index.query.{BoolQueryBuilder, QueryBuilder, QueryBuilders}
import org.elasticsearch.index.reindex.DeleteByQueryAction
import org.elasticsearch.search.SearchHits
import org.elasticsearch.transport.client.PreBuiltTransportClient
import org.slf4j.LoggerFactory


class ElasticsearchClient(hosts: Set[(String, Int)]) {
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val typeName = "_type"
  private val client = new PreBuiltTransportClient(Settings.EMPTY)
  hosts.foreach(x => setTransportAddressToClient(x._1, x._2))
  private val deleteByQueryAction = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
  private val queryBuilder = new BoolQueryBuilder()


  def setTransportAddressToClient(host: String, port: Int) = {
    logger.debug(s"Add a new transport address: '$host:$port' to an elasticsearch client.")
    val transportAddress = new InetSocketTransportAddress(InetAddress.getByName(host), port)
    client.addTransportAddress(transportAddress)
  }

  def doesIndexExist(index: String) = {
    logger.debug(s"Verify the existence of an elasticsearch index: '$index'.")
    val indicesExistsResponse = client.admin().indices().prepareExists(index).execute().actionGet()

    indicesExistsResponse.isExists
  }

  def createIndex(index: String) = {
    logger.info(s"Create a new index: '$index' in Elasticsearch.")
    client.admin().indices().prepareCreate(index).execute().actionGet()
  }

  def deleteDocuments(index: String, documentType: String, query: QueryBuilder = QueryBuilders.matchAllQuery()) = {
    val queryWithType = queryBuilder.must(query).must(QueryBuilders.matchQuery(typeName, documentType))
    deleteByQueryAction
      .filter(queryWithType)
      .source(index)
      .get()
  }

  def deleteIndex(index: String) = {
    logger.info(s"Delete an index: '$index' from Elasticsearch.")
    client.admin().indices().prepareDelete(index).execute().actionGet()
  }

  def search(index: String, documentType: String, queryBuilder: QueryBuilder = QueryBuilders.matchAllQuery()): SearchHits = {
    logger.debug(s"Search the documents by document type: '$documentType' and a query (all by default) in elasticsearch index: '$index'.")
    client
      .prepareSearch(index)
      .setTypes(documentType)
      .setQuery(queryBuilder)
      .setSize(2000)
      .execute().get()
      .getHits
  }

  def write(data: String, index: String, documentType: String, documentId: String = UUID.randomUUID().toString) = {
    logger.debug(s"Write a data: '$data' to an elasticsearch index: '$index'.")
    client
      .prepareIndex(index, documentType, documentId)
      .setSource(data)
      .execute()
      .actionGet()
  }

  def isConnected() = {
    logger.debug(s"Check a connection to an elasticsearch database.")
    client.connectedNodes().size() < 1
  }

  def close() = {
    logger.info(s"Close an elasticsearch database connection.")
    client.close()
  }
}


object a extends App {
    private val client = new PreBuiltTransportClient(Settings.EMPTY)
   val transportAddress = new InetSocketTransportAddress(InetAddress.getByName("176.120.25.19"), 9300)
    client.addTransportAddress(transportAddress)
  private val deleteByQueryAction = DeleteByQueryAction.INSTANCE.newRequestBuilder(client)
  private val queryBuilder = new BoolQueryBuilder()

  val queryWithType=  queryBuilder.must(QueryBuilders.matchQuery("txn",14908651709850000L)).must(QueryBuilders.matchQuery("_type", "es-output"))
    val n = deleteByQueryAction
      .filter(queryWithType)
      .source("test_index_for_output_engine")
      .get()

  println(n.getDeleted)
}