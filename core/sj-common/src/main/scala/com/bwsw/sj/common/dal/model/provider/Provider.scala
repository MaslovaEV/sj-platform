package com.bwsw.sj.common.dal.model.provider

import java.net.{InetSocketAddress, URI, URISyntaxException}
import java.nio.channels.ClosedChannelException
import java.util.Collections
import java.util.concurrent.TimeUnit

import com.aerospike.client.{AerospikeClient, AerospikeException}
import com.bwsw.common.es.ElasticsearchClient
import com.bwsw.sj.common.dal.morphia.MorphiaAnnotations.{IdField, PropertyField}
import com.bwsw.sj.common.dal.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.rest.utils.ValidationUtils
import com.bwsw.sj.common.utils.{MessageResourceUtils, ProviderLiterals}
import com.bwsw.sj.common.utils.ProviderLiterals.types
import com.datastax.driver.core.Cluster
import com.datastax.driver.core.exceptions.NoHostAvailableException
import kafka.javaapi.TopicMetadataRequest
import kafka.javaapi.consumer.SimpleConsumer
import org.apache.zookeeper.ZooKeeper
import org.eclipse.jetty.client.HttpClient
import org.mongodb.morphia.annotations.Entity

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration._
import MessageResourceUtils._
import ValidationUtils._

import scala.util.{Failure, Success, Try}

@Entity("providers")
class Provider(@IdField val name: String,
               val description: String,
               val hosts: Array[String],
               val login: String,
               val password: String,
               @PropertyField("provider-type") val providerType: String) {

  override def equals(obj: scala.Any): Boolean = obj match {
    case that: Provider => that.name.equals(this.name) && that.providerType.equals(this.providerType)
    case _ => false
  }

  def getHosts(): Set[(String, Int)] = {
    val inetSocketAddresses = this.hosts.map { host =>
      val parts = host.split(":")
      (parts(0), parts(1).toInt)
    }.toSet

    inetSocketAddresses
  }

  def validate(): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    val providerDAO = ConnectionRepository.getProviderService

    // 'name' field
    Option(this.name) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Name")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Name")
        }
        else {
          if (!validateName(x)) {
            errors += createMessage("entity.error.incorrect.name", "Provider", x, "provider")
          }

          if (providerDAO.get(x).isDefined) {
            errors += createMessage("entity.error.already.exists", "Provider", x)
          }
        }
    }

    // 'providerType field
    Option(this.providerType) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Type")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.attribute.required", "Type")
        }
        else {
          if (!types.contains(x)) {
            errors += createMessage("entity.error.unknown.type.must.one.of", x, "provider", types.mkString("[", ", ", "]"))
          }
        }
    }

    //'hosts' field
    Option(this.hosts) match {
      case None =>
        errors += createMessage("entity.error.attribute.required", "Hosts")
      case Some(x) =>
        if (x.isEmpty) {
          errors += createMessage("entity.error.hosts.should.be.non.empty")
        } else {
          if (x.head.isEmpty) {
            errors += createMessage("entity.error.attribute.required", "Hosts")
          }
          else {
            for (host <- this.hosts) {
              errors ++= validateHost(host)
            }
          }
        }
    }

    errors
  }

  private def validateHost(host: String): ArrayBuffer[String] = {
    val errors = new ArrayBuffer[String]()
    Try(new URI(s"dummy://${normalizeName(host)}")) match {
      case Success(uri) =>
        val hostname = uri.getHost
        if (hostname == null) {
          errors += createMessage("entity.error.wrong.host", host)
        }

        if (uri.getPort == -1) {
          errors += createMessage("entity.error.host.must.contains.port", host)
        }

        val path = uri.getPath

        if (path.length > 0)
          errors += createMessage("entity.error.host.should.not.contain.uri", path)
      case Failure(_: URISyntaxException) =>
        errors += createMessage("entity.error.wrong.host", host)
      case Failure(e) => throw e
    }

    errors
  }

  def checkConnection(): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    for (host <- this.hosts) {
      errors ++= checkProviderConnectionByType(host, this.providerType)
    }

    errors
  }

  private def checkProviderConnectionByType(host: String, providerType: String): ArrayBuffer[String] = {
    providerType match {
      case ProviderLiterals.cassandraType =>
        checkCassandraConnection(host)
      case ProviderLiterals.aerospikeType =>
        checkAerospikeConnection(host)
      case ProviderLiterals.zookeeperType =>
        checkZookeeperConnection(host)
      case ProviderLiterals.kafkaType =>
        checkKafkaConnection(host)
      case ProviderLiterals.elasticsearchType =>
        checkESConnection(host)
      case ProviderLiterals.jdbcType =>
        checkJdbcConnection(host)
      case ProviderLiterals.restType =>
        checkHttpConnection(host)
      case _ =>
        throw new Exception(s"Host checking for provider type '$providerType' is not implemented")
    }
  }

  private def checkCassandraConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    Try {
      val (host, port) = getHostAndPort(address)

      val builder = Cluster.builder().addContactPointsWithPorts(new InetSocketAddress(host, port))

      val client = builder.build()
      client.getMetadata
      client.close()
    } match {
      case Success(_) =>
      case Failure(_: NoHostAvailableException) =>
        errors += s"Cannot gain an access to Cassandra on '$address'"
      case Failure(_) =>
        errors += s"Wrong host '$address'"
    }

    errors
  }

  private def checkAerospikeConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)

    Try(new AerospikeClient(host, port)) match {
      case Success(client) =>
        if (!client.isConnected) {
          errors += s"Cannot gain an access to Aerospike on '$address'"
        }
        client.close()
      case Failure(_: AerospikeException) =>
        errors += s"Cannot gain an access to Aerospike on '$address'"
      case Failure(e) => throw e
    }

    errors
  }

  private def checkZookeeperConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val zkTimeout = ConnectionRepository.getConfigService.get(ConfigLiterals.zkSessionTimeoutTag).get.value.toInt
    Try(new ZooKeeper(address, zkTimeout, null)) match {
      case Success(client) =>
        val deadline = 1.seconds.fromNow
        var connected: Boolean = false
        while (!connected && deadline.hasTimeLeft) {
          connected = client.getState.isConnected
        }
        if (!connected) {
          errors += s"Can gain an access to Zookeeper on '$address'"
        }
        client.close()
      case Failure(_) =>
        errors += s"Wrong host '$address'"
    }

    errors
  }

  private def checkKafkaConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val (host, port) = getHostAndPort(address)
    val consumer = new SimpleConsumer(host, port, 500, 64 * 1024, "connectionTest")
    val topics = Collections.singletonList("test_connection")
    val req = new TopicMetadataRequest(topics)
    Try(consumer.send(req)) match {
      case Success(_) =>
      case Failure(_: ClosedChannelException) | Failure(_: java.io.EOFException) =>
        errors += s"Can not establish connection to Kafka on '$address'"
      case Failure(_) =>
        errors += s"Some issues encountered while trying to establish connection to '$address'"
    }

    errors
  }

  private def checkESConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val client = new ElasticsearchClient(Set(getHostAndPort(address)))
    if (!client.isConnected()) {
      errors += s"Can not establish connection to ElasticSearch on '$address'"
    }
    client.close()

    errors
  }

  protected def checkJdbcConnection(address: String): ArrayBuffer[String] = ArrayBuffer()

  private def checkHttpConnection(address: String): ArrayBuffer[String] = {
    val errors = ArrayBuffer[String]()
    val uri = new URI("http://" + address)
    val client = new HttpClient()
    val timeout = 10
    client.start()
    Try {
      client
        .newRequest(uri)
        .timeout(timeout, TimeUnit.SECONDS)
        .send()
    } match {
      case Success(_) =>
      case Failure(_) =>
        errors += s"Can not establish connection to REST on '$address'"
    }
    client.stop()

    errors
  }

  private def getHostAndPort(address: String): (String, Int) = {
    val uri = new URI("dummy://" + address)
    val host = uri.getHost()
    val port = uri.getPort()

    (host, port)
  }
}
