package com.bwsw.sj.common.module.regular

import java.util.UUID
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit._

import com.bwsw.common.JsonSerializer
import com.bwsw.sj.common.entities.RegularInstanceMetadata
import com.bwsw.sj.common.module.entities.{TaskParameters, Transaction}
import com.bwsw.sj.common.module.environment.{StatefulModuleEnvironmentManager, ModuleEnvironmentManager}
import com.bwsw.sj.common.module.{ModuleTimer, TaskEnvironmentManager}

import scala.collection.mutable

/**
 * Object responsible for running a task of job
 * Created: 13/04/2016
 * @author Kseniya Mikhaleva
 */

object RegularTaskRunner extends App {

  val serializer = new JsonSerializer()
  val taskParameters = serializer.deserialize[TaskParameters](args(0))
  //обращаться к ксюшиному ресту для получения
  val regularInstanceMetadata = taskParameters.instanceMetadata.asInstanceOf[RegularInstanceMetadata]

  val taskEnvironmentManager = new TaskEnvironmentManager()

  val temporaryOutput = mutable.Map(regularInstanceMetadata.outputs.map(x => (x, mutable.MutableList[Array[Byte]]())): _*)
  val moduleTimer = new ModuleTimer()
  val moduleEnvironmentManager = regularInstanceMetadata.stateManagement match {
    case "none" => new ModuleEnvironmentManager(
      regularInstanceMetadata.options,
      regularInstanceMetadata.outputs,
      temporaryOutput,
      moduleTimer
    )

    case "ram" => new StatefulModuleEnvironmentManager(
      taskEnvironmentManager.getStateStorage(regularInstanceMetadata.stateManagement),
      regularInstanceMetadata.options,
      regularInstanceMetadata.outputs,
      temporaryOutput,
      moduleTimer
    )
  }

  val consumers = taskParameters.inputsWithPartitionRange.map(x => taskEnvironmentManager.createConsumer(x._1, x._2)).toVector
  val producers = regularInstanceMetadata.outputs.map(x => (x, taskEnvironmentManager.createProducer(x))).toMap

  val classLoader = taskEnvironmentManager.getClassLoader(taskParameters.pathToJar)
  val executor = classLoader.loadClass(taskParameters.pathToExecutor)
    .getConstructor(classOf[ModuleEnvironmentManager])
    .newInstance(moduleEnvironmentManager).asInstanceOf[RegularStreamingExecutor]

  executor.init()

  val transactionQueue = new ArrayBlockingQueue[Transaction](taskParameters.queueSize, true)

  new Thread(new Runnable {
    def run() {
      wait(2000)
      transactionQueue.add(Transaction("test_stream", 0, UUID.randomUUID(), "test_consumer",
        List(UUID.randomUUID().toString.getBytes,
          UUID.randomUUID().toString.getBytes,
          UUID.randomUUID().toString.getBytes)
      ))
    }
  }).start()

  //если бы было можно подписаться на consumer, то по появлению новой транзакции - выполнялось следующее:
  while (true) {
    /*
    *
    * 1) создать транзакцию
    * 2) отправить массив байт
    * 3) GroupCheckpoint или по времени (зависит от настройки)
    * 4) executor.onCheckpoint()
    * 5) обнуляем temporaryOutput (вторую составляющую)
    *
    * */
    val transaction: Transaction = transactionQueue.poll(taskParameters.transactionTimeout, MILLISECONDS)
    if (transaction != null) {
      executor.run(transaction)
      temporaryOutput.foreach(x => println(s"producer: ${x._1}, numder of elements: ${x._2.length}"))
      executor.onCheckpoint()
      temporaryOutput.foreach(x => x._2.clear())
    }
    if (moduleTimer.isTime) {
      executor.onTimer()
      moduleTimer.resetTimer()
    }
  }

}


//object TEST extends App {
//val serializer = new ObjectSerializer()
//  val someObject1 = Map("test" -> new test(), "sum" -> 0, "put" -> Some("hello"))
//  val a = serializer.serialize(someObject1)
//val b = serializer.deserialize(a).asInstanceOf[Map[String, Any]]
//
//  println(b("test").asInstanceOf[test].a.sd)
//}
//
//class test extends java.io.Serializable {
//  val s = 5
//  val a = new test1()
//}
//
//class test1 extends java.io.Serializable {
//  val sd = 6
//}