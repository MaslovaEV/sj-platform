package com.bwsw.sj.common.module

import com.bwsw.common.ObjectSerializer
import com.bwsw.sj.common.DAL.repository.ConnectionRepository
import com.bwsw.sj.common.module.DataFactory._

object SjModuleTest extends App {

  System.setProperty("MONGO_HOST", "192.168.1.180")
  System.setProperty("MONGO_PORT", "27017")
  System.setProperty("AEROSPIKE_HOSTS", "127.0.0.1:3000,127.0.0.1:3001,127.0.0.1:3002,127.0.0.1:3003")
  System.setProperty("ZOOKEEPER_HOSTS", "192.168.1.174:2181")
  System.setProperty("REDIS_HOSTS", "127.0.0.1:6379")
  System.setProperty("CASSANDRA_HOST", "127.0.0.1")
  System.setProperty("CASSANDRA_PORT", "9042")

  val streamService = ConnectionRepository.getStreamService
  val objectSerializer = new ObjectSerializer()

  val inputConsumer = createInputConsumer(streamService)
  val outputConsumer = createOutputConsumer(streamService)

  var totalInputElements = 0
  var totalOutputElements = 0

  var inputElements = scala.collection.mutable.ArrayBuffer[Int]()
  var outputElements = scala.collection.mutable.ArrayBuffer[Int]()

  var maybeTxn = inputConsumer.getTransaction
  while (maybeTxn.isDefined) {
    val txn = maybeTxn.get
    while (txn.hasNext()) {
      val element = objectSerializer.deserialize(txn.next()).asInstanceOf[Int]
      inputElements.+=(element)
      totalInputElements += 1
    }
    maybeTxn = inputConsumer.getTransaction
  }

  maybeTxn = outputConsumer.getTransaction
  while (maybeTxn.isDefined) {
    val txn = maybeTxn.get
    while (txn.hasNext()) {
      val element = objectSerializer.deserialize(txn.next()).asInstanceOf[Int]
      outputElements.+=(element)
      totalOutputElements += 1
    }
    maybeTxn = outputConsumer.getTransaction
  }

  assert(totalInputElements == totalOutputElements,
    "Count of all txns elements that are consumed from output stream should equals count of all txns elements that are consumed from input stream")

  assert(inputElements == outputElements,
    "All txns elements that are consumed from output stream should equals all txns elements that are consumed from input stream")

  close()
  ConnectionRepository.close()

  println("DONE")

  //  "All txns elements that are consumed from output stream" should "equals all txns elements that are consumed from input stream" in {
  //    val streamService = ConnectionRepository.getStreamService
  //    val objectSerializer = new ObjectSerializer()
  //
  //    val inputConsumer = createInputConsumer(streamService)
  //    val outputConsumer = createOutputConsumer(streamService)
  //
  //
  //
  //    var maybeTxn = inputConsumer.getTransaction
  //    while (maybeTxn.isDefined) {
  //      val txn = maybeTxn.get
  //      while (txn.hasNext()) {
  //        val element = objectSerializer.deserialize(txn.next()).asInstanceOf[String].toInt
  //        inputElements.+=(element)
  //      }
  //      maybeTxn = inputConsumer.getTransaction
  //    }
  //
  //    maybeTxn = outputConsumer.getTransaction
  //    while (maybeTxn.isDefined) {
  //      val txn = maybeTxn.get
  //      while (txn.hasNext()) {
  //        val element = objectSerializer.deserialize(txn.next()).asInstanceOf[String].toInt
  //        outputElements.+=(element)
  //      }
  //      maybeTxn = outputConsumer.getTransaction
  //    }
  //
  //    inputElements should contain theSameElementsAs outputElements
  //
  //    close()
  //    ConnectionRepository.close()
  //
  //    println("DONE")
  //  }

}