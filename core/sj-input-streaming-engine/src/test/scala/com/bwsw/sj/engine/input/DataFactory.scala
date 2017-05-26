package com.bwsw.sj.engine.input

import java.io.{BufferedReader, File, InputStreamReader}
import java.util
import java.util.jar.JarFile

import com.bwsw.common.JsonSerializer
import com.bwsw.common.file.utils.FileStorage
import com.bwsw.sj.common.config.BenchmarkConfigNames
import com.bwsw.sj.common.dal.model.instance.{InputInstanceDomain, InputTask, InstanceDomain}
import com.bwsw.sj.common.dal.model.provider.ProviderDomain
import com.bwsw.sj.common.dal.model.service.{ServiceDomain, TStreamServiceDomain, ZKServiceDomain}
import com.bwsw.sj.common.dal.model.stream.{StreamDomain, TStreamStreamDomain}
import com.bwsw.sj.common.dal.repository.GenericMongoRepository
import com.bwsw.sj.common.utils.{ProviderLiterals, _}
import com.bwsw.sj.engine.core.testutils.TestStorageServer
import com.bwsw.tstreams.agents.consumer.Consumer
import com.bwsw.tstreams.agents.consumer.Offset.Oldest
import com.bwsw.tstreams.env.{ConfigurationOptions, TStreamsFactory}
import com.typesafe.config.ConfigFactory

import scala.util.{Failure, Success, Try}

object DataFactory {
  private val config = ConfigFactory.load()
  private val zookeeperHosts = config.getString(BenchmarkConfigNames.zkHosts)
  private val testNamespace = "test_namespace_for_input_engine"
  private val instanceName = "test-instance-for-input-engine"
  private val zookeeperProviderName = "zookeeper-test-provider"
  private val tstreamServiceName = "tstream-test-service"
  private val zookeeperServiceName = "zookeeper-test-service"
  private val tstreamOutputNamePrefix = "tstream-output"
  private var instanceOutputs: Array[String] = Array()
  private val tasks = new util.HashMap[String, InputTask]()
  tasks.put(s"$instanceName-task0", new InputTask(SjInputServices.host, SjInputServices.port))
  private val partitions = 1
  private val serializer = new JsonSerializer()
  private val zookeeperProvider = new ProviderDomain(zookeeperProviderName, zookeeperProviderName, zookeeperHosts.split(","), "", "", ProviderLiterals.zookeeperType)
  private val tstrqService = new TStreamServiceDomain(tstreamServiceName, tstreamServiceName, zookeeperProvider, TestStorageServer.prefix, TestStorageServer.token)
  private val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()
  val storageClient = tstreamFactory.getStorageClient()

  val outputCount = 2

  private def setTStreamFactoryProperties() = {
    setAuthOptions(tstrqService)
    setStorageOptions(tstrqService)
    setCoordinationOptions(tstrqService)
  }

  private def setAuthOptions(tStreamService: TStreamServiceDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Auth.key, tStreamService.token)
  }

  private def setStorageOptions(tStreamService: TStreamServiceDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.StorageClient.Zookeeper.endpoints, tStreamService.provider.hosts.mkString(","))
      .setProperty(ConfigurationOptions.StorageClient.Zookeeper.prefix, tStreamService.prefix)
  }

  private def setCoordinationOptions(tStreamService: TStreamServiceDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.Coordination.endpoints, tStreamService.provider.hosts.mkString(","))
  }

  def createProviders(providerService: GenericMongoRepository[ProviderDomain]) = {
    providerService.save(zookeeperProvider)
  }

  def deleteProviders(providerService: GenericMongoRepository[ProviderDomain]) = {
    providerService.delete(zookeeperProviderName)
  }

  def createServices(serviceManager: GenericMongoRepository[ServiceDomain], providerService: GenericMongoRepository[ProviderDomain]) = {
    val zkService = new ZKServiceDomain(zookeeperServiceName, zookeeperServiceName, zookeeperProvider, testNamespace)
    serviceManager.save(zkService)

    serviceManager.save(tstrqService)
  }

  def deleteServices(serviceManager: GenericMongoRepository[ServiceDomain]) = {
    serviceManager.delete(zookeeperServiceName)
    serviceManager.delete(tstreamServiceName)
  }

  def createStreams(sjStreamService: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], outputCount: Int) = {
    (1 to outputCount).foreach(x => {
      createOutputTStream(sjStreamService, serviceManager, partitions, x.toString)
      instanceOutputs = instanceOutputs :+ (tstreamOutputNamePrefix + x)
    })
  }

  def deleteStreams(streamService: GenericMongoRepository[StreamDomain], outputCount: Int) = {
    (1 to outputCount).foreach(x => deleteOutputTStream(streamService, x.toString))
  }

  private def createOutputTStream(sjStreamService: GenericMongoRepository[StreamDomain], serviceManager: GenericMongoRepository[ServiceDomain], partitions: Int, suffix: String) = {
    val s2 = new TStreamStreamDomain(
      tstreamOutputNamePrefix + suffix,
      tstrqService,
      partitions,
      tstreamOutputNamePrefix + suffix,
      false,
      Array("output", "some tags")
    )

    sjStreamService.save(s2)

    storageClient.createStream(
      tstreamOutputNamePrefix + suffix,
      partitions,
      1000 * 60,
      "description of test output tstream")
  }

  private def deleteOutputTStream(streamService: GenericMongoRepository[StreamDomain], suffix: String) = {
    streamService.delete(tstreamOutputNamePrefix + suffix)

    storageClient.deleteStream(tstreamOutputNamePrefix + suffix)
  }

  def createInstance(serviceManager: GenericMongoRepository[ServiceDomain],
                     instanceService: GenericMongoRepository[InstanceDomain],
                     checkpointInterval: Int
                    ) = {

    val instance = new InputInstanceDomain(instanceName, EngineLiterals.inputStreamingType,
      "input-streaming-stub", "1.0", "com.bwsw.input.streaming.engine-1.0",
      serviceManager.get(zookeeperServiceName).get.asInstanceOf[ZKServiceDomain], EngineLiterals.everyNthMode
    )
    instance.status = EngineLiterals.started
    instance.description = "some description of test instance"
    instance.outputs = instanceOutputs
    instance.checkpointInterval = checkpointInterval
    instance.duplicateCheck = false
    instance.lookupHistory = 100
    instance.queueMaxSize = 500
    instance.defaultEvictionPolicy = EngineLiterals.lruDefaultEvictionPolicy
    instance.evictionPolicy = "expanded-time"
    instance.tasks = tasks

    instanceService.save(instance)
  }

  def deleteInstance(instanceService: GenericMongoRepository[InstanceDomain]) = {
    instanceService.delete(instanceName)
  }

  def loadModule(file: File, storage: FileStorage) = {
    val builder = new StringBuilder
    val jar = new JarFile(file)
    val enu = jar.entries()
    while (enu.hasMoreElements) {
      val entry = enu.nextElement
      if (entry.getName.equals("specification.json")) {
        val reader = new BufferedReader(new InputStreamReader(jar.getInputStream(entry), "UTF-8"))
        val result = Try {
          var line = reader.readLine
          while (Option(line).isDefined) {
            builder.append(line + "\n")
            line = reader.readLine
          }
        }
        reader.close()
        result match {
          case Success(_) =>
          case Failure(e) => throw e
        }
      }
    }

    val specification = serializer.deserialize[Map[String, Any]](builder.toString())

    storage.put(file, file.getName, specification, "module")
  }

  def deleteModule(storage: FileStorage, filename: String) = {
    storage.delete(filename)
  }

  def createOutputConsumer(streamService: GenericMongoRepository[StreamDomain], suffix: String) = {
    createConsumer(tstreamOutputNamePrefix + suffix, streamService)
  }

  private def createConsumer(streamName: String, streamService: GenericMongoRepository[StreamDomain]): Consumer = {
    val stream = streamService.get(streamName).get.asInstanceOf[TStreamStreamDomain]

    setStreamOptions(stream)

    tstreamFactory.getConsumer(
      streamName,
      (0 until stream.partitions).toSet,
      Oldest)
  }

  protected def setStreamOptions(stream: TStreamStreamDomain) = {
    tstreamFactory.setProperty(ConfigurationOptions.Stream.name, stream.name)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.partitionsCount, stream.partitions)
    tstreamFactory.setProperty(ConfigurationOptions.Stream.description, stream.description)
  }
}
