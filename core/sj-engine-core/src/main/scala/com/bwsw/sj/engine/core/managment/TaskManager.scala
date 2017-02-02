package com.bwsw.sj.engine.core.managment

import java.io.File
import java.net.URLClassLoader

import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.model.module._
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.config.ConfigLiterals
import com.bwsw.sj.common.config.ConfigurationSettingsUtils._
import com.bwsw.sj.common.engine.StreamingExecutor
import com.bwsw.sj.common.rest.entities.module.ExecutionPlan
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.common.utils.StreamLiterals._
import com.bwsw.sj.engine.core.converter.ArrayByteConverter
import com.bwsw.sj.engine.core.environment.EnvironmentManager
import com.bwsw.tstreams.agents.consumer.Offset.IOffset
import com.bwsw.tstreams.agents.consumer.subscriber.Callback
import com.bwsw.tstreams.env.{TSF_Dictionary, TStreamsFactory}
import com.bwsw.tstreams.streams.StreamService
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.collection.mutable

abstract class TaskManager() {
  protected val logger = LoggerFactory.getLogger(this.getClass)

  val streamDAO = ConnectionRepository.getStreamService

  require(System.getenv("INSTANCE_NAME") != null &&
    System.getenv("TASK_NAME") != null &&
    System.getenv("AGENTS_HOST") != null &&
    System.getenv("AGENTS_PORTS") != null,
    "No environment variables: INSTANCE_NAME, TASK_NAME, AGENTS_HOST, AGENTS_PORTS")

  val instanceName = System.getenv("INSTANCE_NAME")
  val agentsHost = System.getenv("AGENTS_HOST")
  private val agentsPorts = System.getenv("AGENTS_PORTS").split(",").map(_.toInt)
  protected val numberOfAgentsPorts = agentsPorts.length
  val taskName = System.getenv("TASK_NAME")
  val instance: Instance = getInstance()
  protected val auxiliarySJTStream = getAuxiliaryTStream()
  protected val auxiliaryTStreamService = getAuxiliaryTStreamService()
  val tstreamFactory = new TStreamsFactory()
  setTStreamFactoryProperties()

  protected var currentPortNumber = 0
  private val storage = ConnectionRepository.getFileStorage

  protected val fileMetadata: FileMetadata = ConnectionRepository.getFileMetadataService.getByParameters(
    Map("specification.name" -> instance.moduleName,
      "specification.module-type" -> instance.moduleType,
      "specification.version" -> instance.moduleVersion)
  ).head
  protected val executorClassName = fileMetadata.specification.executorClass

  protected val moduleClassLoader = createClassLoader()

  val converter = new ArrayByteConverter
  val inputs: mutable.Map[SjStream, Array[Int]]

  private def getInstance() = {
    val maybeInstance = ConnectionRepository.getInstanceService.get(instanceName)
    var instance: Instance = null
    if (maybeInstance.isDefined) {
      instance = maybeInstance.get

      if (instance.status != started) {
        throw new InterruptedException(s"Task cannot be started because of '${instance.status}' status of instance")
      }
    }
    else throw new NoSuchElementException(s"Instance is named '$instanceName' has not found")

    instance
  }

  private def getAuxiliaryTStream() = {
    val inputs = instance.getInputsWithoutStreamMode()
    val streams = inputs.union(instance.outputs)
    val sjStream = streams.flatMap(s => streamDAO.get(s)).filter(s => s.streamType.equals(tstreamType)).head

    sjStream
  }

  private def getAuxiliaryTStreamService() = {
    auxiliarySJTStream.service.asInstanceOf[TStreamService]
  }

  private def setTStreamFactoryProperties() = {
    setMetadataClusterProperties(auxiliaryTStreamService)
    setDataClusterProperties(auxiliaryTStreamService)
    setCoordinationOptions(auxiliaryTStreamService)
    setBindHostForAgents()
    setPersistentQueuePath()
    applyConfigurationSettings()
  }

  private def setMetadataClusterProperties(tStreamService: TStreamService) = {
//    logger.debug(s"Task name: $taskName. Set properties of metadata storage " +
//      s"(namespace: ${tStreamService.metadataNamespace}, hosts: ${tStreamService.metadataProvider.hosts.mkString(",")}) " +
//      s"of t-stream factory\n")
//    tstreamFactory.setProperty(TSF_Dictionary.Metadata.Cluster.NAMESPACE, tStreamService.metadataNamespace)
//      .setProperty(TSF_Dictionary.Metadata.Cluster.ENDPOINTS, tStreamService.metadataProvider.hosts.mkString(","))
  }

  private def setDataClusterProperties(tStreamService: TStreamService) = {
//    tStreamService.dataProvider.providerType match {
//      case ProviderLiterals.aerospikeType =>
//        logger.debug(s"Task name: $taskName. Set properties of aerospike data storage " +
//          s"(namespace: ${tStreamService.dataNamespace}, hosts: ${tStreamService.dataProvider.hosts.mkString(",")}) " +
//          s"of t-stream factory\n")
//        tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.DRIVER, TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_AEROSPIKE)
//      case _ =>
//        logger.debug(s"Task name: $taskName. Set properties of cassandra data storage " +
//          s"(namespace: ${tStreamService.dataNamespace}, hosts: ${tStreamService.dataProvider.hosts.mkString(",")}) " +
//          s"of t-stream factory\n")
//        tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.DRIVER, TSF_Dictionary.Data.Cluster.Consts.DATA_DRIVER_CASSANDRA)
//    }
//
//    tstreamFactory.setProperty(TSF_Dictionary.Data.Cluster.NAMESPACE, tStreamService.dataNamespace)
//      .setProperty(TSF_Dictionary.Data.Cluster.ENDPOINTS, tStreamService.dataProvider.hosts.mkString(","))
  }

  private def setCoordinationOptions(tStreamService: TStreamService) = {
//    tstreamFactory.setProperty(TSF_Dictionary.Coordination.ROOT, s"/${tStreamService.lockNamespace}")
//      .setProperty(TSF_Dictionary.Coordination.ENDPOINTS, tStreamService.lockProvider.hosts.mkString(","))
  }

  private def setBindHostForAgents() = {
    tstreamFactory.setProperty(TSF_Dictionary.Producer.BIND_HOST, agentsHost)
    tstreamFactory.setProperty(TSF_Dictionary.Consumer.Subscriber.BIND_HOST, agentsHost)
  }

  private def setPersistentQueuePath() = {
    tstreamFactory.setProperty(TSF_Dictionary.Consumer.Subscriber.PERSISTENT_QUEUE_PATH, "/tmp/" + persistentQueuePath)
  }

  private def applyConfigurationSettings() = {
    val configService = ConnectionRepository.getConfigService

    val tstreamsSettings = configService.getByParameters(Map("domain" -> ConfigLiterals.tstreamsDomain))
    tstreamsSettings.foreach(x => tstreamFactory.setProperty(clearConfigurationSettingName(x.domain, x.name), x.value))
  }

  /**
    * Returns class loader for retrieving classes from jar
    *
    * @return Class loader for retrieving classes from jar
    */
  protected def createClassLoader() = {
    val file = getModuleJar
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Get class loader for jar file: ${file.getName}\n")

    val classLoaderUrls = Array(file.toURI.toURL)

    new URLClassLoader(classLoaderUrls, ClassLoader.getSystemClassLoader)
  }

  private def getModuleJar: File = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. Get file contains uploaded '${instance.moduleName}' module jar\n")
    storage.get(fileMetadata.filename, s"tmp/${instance.moduleName}")
  }

  @throws(classOf[Exception])
  protected def getInputs(executionPlan: ExecutionPlan) = {
    val task = executionPlan.tasks.get(taskName)
    task match {
      case null => throw new NullPointerException("There is no task with that name in the execution plan.")
      case _ => task.inputs.asScala.map(x => (streamDAO.get(x._1).get, x._2))
    }
  }

  /**
    * Create t-stream producers for each output stream
    *
    * @return Map where key is stream name and value is t-stream producer
    */
  protected def createOutputProducers() = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create the t-stream producers for each output stream\n")

    tstreamFactory.setProperty(TSF_Dictionary.Producer.Transaction.DATA_WRITE_BATCH_SIZE, 20)

    instance.outputs
      .map(x => (x, streamDAO.get(x).get))
      .map(x => (x._1, createProducer(x._2.asInstanceOf[TStreamSjStream]))).toMap
  }

  def createProducer(stream: TStreamSjStream) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create producer for stream: ${stream.name}\n")

    setProducerBindPort()
    setStreamOptions(stream)

    tstreamFactory.getProducer[Array[Byte]](
      "producer_for_" + taskName + "_" + stream.name,
      null, //todo after integration with t-streams
      converter,
      (0 until stream.partitions).toSet)
  }

  private def setProducerBindPort() = {
    tstreamFactory.setProperty(TSF_Dictionary.Producer.BIND_PORT, agentsPorts(currentPortNumber))
    currentPortNumber += 1
  }

  def createTStreamOnCluster(name: String, description: String, partitions: Int): Unit = {
    val streamTTL = tstreamFactory.getProperty(TSF_Dictionary.Stream.TTL).asInstanceOf[Int]
    tstreamFactory.setProperty(TSF_Dictionary.Stream.NAME, auxiliarySJTStream.name)
    val auxiliaryTStream = tstreamFactory.getStream()
    val metadataStorage = auxiliaryTStream.metadataStorage
    val dataStorage = auxiliaryTStream.dataStorage

    if (!StreamService.isExist(name, metadataStorage)) {
      logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
        s"Create t-stream: $name to $description\n")
      StreamService.createStream(
        name,
        partitions,
        streamTTL,
        description,
        metadataStorage,
        dataStorage
      )
    }
  }

  def getSjStream(name: String, description: String, tags: Array[String], partitions: Int) = {
    new TStreamSjStream(
      name,
      description,
      partitions,
      auxiliaryTStreamService,
      tstreamType,
      tags
    )
  }

  /**
    * Creates a t-stream consumer with pub/sub property
    *
    * @param stream     SjStream from which massages are consumed
    * @param partitions Range of stream partition
    * @param offset     Offset policy that describes where a consumer starts
    * @param callback   Subscriber callback for t-stream consumer
    * @return T-stream subscribing consumer
    */
  def createSubscribingConsumer(stream: TStreamSjStream,
                                partitions: List[Int],
                                offset: IOffset,
                                callback: Callback[Array[Byte]]) = {
    logger.debug(s"Instance name: $instanceName, task name: $taskName. " +
      s"Create subscribing consumer for stream: ${stream.name} (partitions from ${partitions.head} to ${partitions.tail.head})\n")

    val partitionRange = (partitions.head to partitions.tail.head).toSet

    setStreamOptions(stream)
    setSubscribingConsumerBindPort()

    tstreamFactory.getSubscriber[Array[Byte]](
      "subscribing_consumer_for_" + taskName + "_" + stream.name,
      null, //todo after integration with t-streams
      converter,
      partitionRange,
      callback,
      offset)
  }

  protected def setStreamOptions(stream: TStreamSjStream) = {
    tstreamFactory.setProperty(TSF_Dictionary.Stream.NAME, stream.name)
    tstreamFactory.setProperty(TSF_Dictionary.Stream.PARTITIONS, stream.partitions)
    tstreamFactory.setProperty(TSF_Dictionary.Stream.DESCRIPTION, stream.description)
  }

  private def setSubscribingConsumerBindPort() = {
    tstreamFactory.setProperty(TSF_Dictionary.Consumer.Subscriber.BIND_PORT, agentsPorts(currentPortNumber))
    currentPortNumber += 1
  }

  /**
    * @return An instance of executor of module that has got an environment manager
    */
  def getExecutor(environmentManager: EnvironmentManager): StreamingExecutor
}
